package com.vicinity24.api.linked.store.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vicinity24.api.core.storage.StorageManager;
import com.vicinity24.api.linked.store.entity.Store;
import com.vicinity24.api.linked.store.entity.StoreCategory;
import com.vicinity24.api.linked.store.entity.StoreProduct;
import com.vicinity24.api.linked.store.entity.StoreProductVariant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class LinkedStoreRealImageService {
    private static final Logger log = LoggerFactory.getLogger(LinkedStoreRealImageService.class);
    private static final String USER_AGENT = "Vicinity24-LinkedStoreSeeder/1.0";

    private final StorageManager storageManager;
    private final HttpClient httpClient;
    private final ConcurrentMap<String, List<String>> imageCache = new ConcurrentHashMap<>();

    public LinkedStoreRealImageService(StorageManager storageManager, ObjectMapper objectMapper) {
        this.storageManager = storageManager;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public String resolveStoreBanner(Store store) {
        String storeSlug = sanitizeKeyPart(store.getSlug());
        return resolveImageSet(
                "store-banner:" + storeSlug,
                "linked-store/stores/" + storeSlug + "/banner",
                storeBannerUrls(storeSlug),
                1
        ).get(0);
    }

    public String resolveCategoryBanner(StoreCategory category) {
        String parentName = category.getParent() == null ? category.getStore().getName() : category.getParent().getName();
        return resolveCategoryBanner(category.getStore(), category.getSlug(), category.getName(), parentName);
    }

    public String resolveCategoryBanner(Store store, String categorySlug, String categoryName, String parentName) {
        String storeSlug = sanitizeKeyPart(store.getSlug());
        String normalizedCategorySlug = sanitizeKeyPart(categorySlug);
        return resolveImageSet(
                "category-banner:" + storeSlug + ":" + normalizedCategorySlug,
                "linked-store/categories/" + storeSlug + "/" + normalizedCategorySlug + "/banner",
                categoryBannerUrls(normalizedCategorySlug),
                1
        ).get(0);
    }

    public List<String> resolveProductImages(StoreProduct product) {
        return resolveProductImages(
                product.getStore(),
                product.getCategory(),
                product.getSku(),
                product.getName(),
                product.getProperties()
        );
    }

    public List<String> resolveProductImages(
            Store store,
            StoreCategory category,
            String productSku,
            String productName,
            Map<String, Object> properties
    ) {
        String storeSlug = sanitizeKeyPart(store.getSlug());
        String normalizedSku = sanitizeKeyPart(productSku);
        ProductKind productKind = productKind(productSku, productName);
        return resolveImageSet(
                "product:" + storeSlug + ":" + normalizedSku,
                "linked-store/products/" + storeSlug + "/" + normalizedSku + "/gallery",
                productImageUrls(normalizedSku, productKind),
                3
        );
    }

    public List<String> resolveVariantImages(StoreProductVariant variant) {
        return resolveVariantImages(
                variant.getStore(),
                variant.getProduct().getCategory(),
                variant.getProduct().getSku(),
                variant.getProduct().getName(),
                variant.getSku(),
                variant.getOptions()
        );
    }

    public List<String> resolveVariantImages(
            Store store,
            StoreCategory category,
            String productSku,
            String productName,
            String variantSku,
            Map<String, Object> variantOptions
    ) {
        String storeSlug = sanitizeKeyPart(store.getSlug());
        String normalizedProductSku = sanitizeKeyPart(productSku);
        String normalizedVariantSku = sanitizeKeyPart(variantSku);
        ProductKind productKind = productKind(productSku, productName);
        String visualDescriptor = visualDescriptor(variantOptions);
        List<String> variantUrls = visualDescriptor == null
                ? List.of()
                : variantImageUrls(normalizedProductSku, productKind, sanitizeKeyPart(visualDescriptor));
        if (variantUrls.isEmpty()) {
            return resolveProductImages(store, category, productSku, productName, Map.of());
        }

        return resolveImageSet(
                "variant:" + storeSlug + ":" + normalizedVariantSku,
                "linked-store/variants/" + storeSlug + "/" + normalizedProductSku + "/" + normalizedVariantSku + "/gallery",
                variantUrls,
                3
        );
    }

    private List<String> resolveImageSet(String cacheKey, String objectKeyBase, List<String> remoteUrls, int desiredCount) {
        List<String> cached = imageCache.get(cacheKey);
        if (isUsableImageSet(cached)) {
            return cached;
        }

        List<String> refreshed = fetchAndStoreImages(objectKeyBase, remoteUrls, desiredCount);
        imageCache.put(cacheKey, refreshed);
        return refreshed;
    }

    private List<String> fetchAndStoreImages(String objectKeyBase, List<String> remoteUrls, int desiredCount) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>(remoteUrls);
        if (candidates.isEmpty()) {
            candidates.addAll(emergencyFallbackUrls());
        }

        List<String> uploaded = new ArrayList<>();
        int index = 1;
        for (String remoteUrl : candidates) {
            String uploadedUrl = downloadAndUpload(remoteUrl, objectKeyBase + "-" + index);
            if (uploadedUrl == null) {
                continue;
            }
            uploaded.add(uploadedUrl);
            index++;
            if (uploaded.size() >= desiredCount) {
                break;
            }
        }

        if (!uploaded.isEmpty()) {
            return uploaded;
        }

        List<String> fallback = candidates.stream().limit(Math.max(1, desiredCount)).toList();
        log.warn("real_image_upload_fallback key={} urls={}", objectKeyBase, fallback.size());
        return fallback;
    }

    private boolean isUsableImageSet(List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            return false;
        }
        for (String url : urls) {
            if (url == null || url.isBlank()) {
                return false;
            }
            if (isR2Url(url) && !remoteObjectExists(url)) {
                return false;
            }
        }
        return true;
    }

    private boolean isR2Url(String url) {
        return url.contains(".r2.dev/");
    }

    private boolean remoteObjectExists(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("User-Agent", USER_AGENT)
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() < 400) {
                return true;
            }
            if (response.statusCode() != 405) {
                log.warn("image_probe_failed status={} url={}", response.statusCode(), url);
                return false;
            }
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.warn("image_probe_exception url={} error={}", url, exception.getMessage());
            return false;
        }

        try {
            HttpRequest fallbackRequest = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("User-Agent", USER_AGENT)
                    .GET()
                    .build();
            HttpResponse<Void> fallbackResponse = httpClient.send(fallbackRequest, HttpResponse.BodyHandlers.discarding());
            boolean exists = fallbackResponse.statusCode() < 400;
            if (!exists) {
                log.warn("image_probe_fallback_failed status={} url={}", fallbackResponse.statusCode(), url);
            }
            return exists;
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.warn("image_probe_fallback_exception url={} error={}", url, exception.getMessage());
            return false;
        }
    }

    private String downloadAndUpload(String remoteUrl, String objectKeyBase) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(remoteUrl))
                    .timeout(Duration.ofSeconds(30))
                    .header("User-Agent", USER_AGENT)
                    .GET()
                    .build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() >= 400) {
                log.warn("image_download_failed status={} url={}", response.statusCode(), remoteUrl);
                return null;
            }

            String contentType = response.headers().firstValue("Content-Type").orElse("image/jpeg");
            if (!contentType.startsWith("image/")) {
                log.warn("image_download_invalid_content_type url={} contentType={}", remoteUrl, contentType);
                return null;
            }

            String objectKey = objectKeyBase + extensionFor(contentType, remoteUrl);
            return storageManager.uploadBytes(objectKey, response.body(), contentType);
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.warn("image_download_exception url={} error={}", remoteUrl, exception.getMessage());
            return null;
        }
    }

    private String extensionFor(String contentType, String remoteUrl) {
        if (contentType.contains("png")) {
            return ".png";
        }
        if (contentType.contains("webp")) {
            return ".webp";
        }
        if (contentType.contains("gif")) {
            return ".gif";
        }
        String normalizedUrl = remoteUrl.toLowerCase(Locale.ROOT);
        if (normalizedUrl.contains(".png")) {
            return ".png";
        }
        if (normalizedUrl.contains(".webp")) {
            return ".webp";
        }
        return ".jpg";
    }

    private List<String> storeBannerUrls(String storeSlug) {
        return switch (storeSlug) {
            case "tech-hub-europe" -> urls(
                    "https://cdn.stocksnap.io/img-thumbs/960w/I9INCIK831.jpg"
            );
            case "urban-home-living" -> urls(
                    "https://live.staticflickr.com/4656/39953731391_ca612a812a_b.jpg"
            );
            case "active-outdoors-pro" -> urls(
                    "https://cdn.stocksnap.io/img-thumbs/960w/PTERNSNX0F.jpg"
            );
            case "style-lab-fashion" -> urls(
                    "https://cdn.stocksnap.io/img-thumbs/960w/645GI8G1W4.jpg"
            );
            default -> emergencyFallbackUrls();
        };
    }

    private List<String> categoryBannerUrls(String categorySlug) {
        return switch (categorySlug) {
            case "electronics", "smartphones" -> urls(
                    "https://pd.w.org/2026/06/2086a2ac94c64ec74.45914128-1536x2048.jpg"
            );
            case "laptops" -> urls(
                    "https://cdn.stocksnap.io/img-thumbs/960w/FG432B2OT4.jpg"
            );
            case "audio" -> urls(
                    "https://images.rawpixel.com/editor_1024/czNmcy1wcml2YXRlL3Jhd3BpeGVsX2ltYWdlcy93ZWJzaXRlX2NvbnRlbnQvbHIvaXMxMzIwMy1pbWFnZS1rd3lzZTFnci5qcGc.jpg"
            );
            case "home", "sofas" -> urls(
                    "https://live.staticflickr.com/2829/10798290264_ea1fd4754d_b.jpg"
            );
            case "dining" -> urls(
                    "https://upload.wikimedia.org/wikipedia/commons/8/8c/Mahogany_dining_table_attributed_to_Duncan_Phyfe%2C_c._1815%2C_Dayton_Art_Institute.JPG"
            );
            case "kitchen-appliances" -> urls(
                    "https://live.staticflickr.com/41/87881542_7ad8398828_b.jpg"
            );
            case "outdoor", "bikes" -> urls(
                    "https://live.staticflickr.com/771/33421391135_319aac8b07_b.jpg"
            );
            case "camping" -> urls(
                    "https://cdn.stocksnap.io/img-thumbs/960w/177F2EBDDC.jpg"
            );
            case "paddling" -> urls(
                    "https://live.staticflickr.com/895/41432798741_23875e1597_b.jpg"
            );
            case "fashion", "sneakers" -> urls(
                    "https://cdn.stocksnap.io/img-thumbs/960w/645GI8G1W4.jpg"
            );
            case "outerwear" -> urls(
                    "https://images.rawpixel.com/editor_1024/czNmcy1wcml2YXRlL3Jhd3BpeGVsX2ltYWdlcy93ZWJzaXRlX2NvbnRlbnQvbHIvdXB3azYxNzQxOTQzLXdpa2ltZWRpYS1pbWFnZS1rb3diZHVwcS5qcGc.jpg"
            );
            case "watches" -> urls(
                    "https://live.staticflickr.com/3324/3575148810_88749bec8e_b.jpg"
            );
            default -> emergencyFallbackUrls();
        };
    }

    private List<String> productImageUrls(String productSku, ProductKind productKind) {
        return switch (productSku) {
            case "tech-phn-novax" -> urls(
                    "https://pd.w.org/2026/06/2086a2ac94c64ec74.45914128-1536x2048.jpg",
                    "https://live.staticflickr.com/3849/18998496565_d7844460e3.jpg",
                    "https://live.staticflickr.com/5696/22389367255_6ba8a17d76_b.jpg"
            );
            case "tech-lap-wb14" -> urls(
                    "https://cdn.stocksnap.io/img-thumbs/960w/FG432B2OT4.jpg",
                    "https://cdn.stocksnap.io/img-thumbs/960w/8PUO9PFLV2.jpg",
                    "https://live.staticflickr.com/2307/2193891309_516400c479_b.jpg"
            );
            case "tech-aud-spm" -> urls(
                    "https://images.rawpixel.com/editor_1024/czNmcy1wcml2YXRlL3Jhd3BpeGVsX2ltYWdlcy93ZWJzaXRlX2NvbnRlbnQvbHIvaXMxMzIwMy1pbWFnZS1rd3lzZTFnci5qcGc.jpg",
                    "https://images.rawpixel.com/editor_1024/czNmcy1wcml2YXRlL3Jhd3BpeGVsX2ltYWdlcy93ZWJzaXRlX2NvbnRlbnQvbHIvcHg4NjA2ODUtaW1hZ2Uta3d2dXZ5d24uanBn.jpg",
                    "https://live.staticflickr.com/5763/22921682035_334b6161c9_b.jpg"
            );
            case "home-sof-cloud" -> urls(
                    "https://live.staticflickr.com/65535/48089881637_86af42f066_b.jpg",
                    "https://live.staticflickr.com/4656/39953731391_ca612a812a_b.jpg",
                    "https://live.staticflickr.com/2829/10798290264_ea1fd4754d_b.jpg"
            );
            case "home-din-nord" -> urls(
                    "https://upload.wikimedia.org/wikipedia/commons/8/8c/Mahogany_dining_table_attributed_to_Duncan_Phyfe%2C_c._1815%2C_Dayton_Art_Institute.JPG",
                    "https://upload.wikimedia.org/wikipedia/commons/b/be/Drop-leaf_dining_table_MET_DP104721.jpg",
                    "https://upload.wikimedia.org/wikipedia/commons/0/01/William_H._Edwards%2C_Dining_Table%2C_1935-1942%2C_NGA_17904.jpg"
            );
            case "home-kit-barista" -> urls(
                    "https://live.staticflickr.com/41/87881542_7ad8398828_b.jpg",
                    "https://live.staticflickr.com/1477/25299515990_e4e6706627_b.jpg",
                    "https://live.staticflickr.com/830/27715495688_cec57a9141_b.jpg"
            );
            case "out-bik-tbcrb" -> urls(
                    "https://live.staticflickr.com/771/33421391135_319aac8b07_b.jpg",
                    "https://live.staticflickr.com/3839/33265683642_7787075506_b.jpg",
                    "https://live.staticflickr.com/3699/33421401745_b33002e0a2_b.jpg"
            );
            case "out-cmp-summit" -> urls(
                    "https://cdn.stocksnap.io/img-thumbs/960w/177F2EBDDC.jpg",
                    "https://cdn.stocksnap.io/img-thumbs/960w/OCXLY7U3OG.jpg",
                    "https://cdn.stocksnap.io/img-thumbs/960w/PTERNSNX0F.jpg"
            );
            case "out-pad-riverrun" -> urls(
                    "https://live.staticflickr.com/895/41432798741_23875e1597_b.jpg",
                    "https://live.staticflickr.com/811/41432796971_87f5035ab8_b.jpg",
                    "https://live.staticflickr.com/3739/11640366094_31bbf639ca_b.jpg"
            );
            case "fash-snk-aerost" -> urls(
                    "https://live.staticflickr.com/4354/36218260284_d07ee0f055_b.jpg",
                    "https://cdn.stocksnap.io/img-thumbs/960w/645GI8G1W4.jpg",
                    "https://upload.wikimedia.org/wikipedia/commons/f/f5/Running_shoes_display.JPG"
            );
            case "fash-out-alpine" -> urls(
                    "https://images.rawpixel.com/editor_1024/czNmcy1wcml2YXRlL3Jhd3BpeGVsX2ltYWdlcy93ZWJzaXRlX2NvbnRlbnQvbHIvdXB3azYxNzQxOTQzLXdpa2ltZWRpYS1pbWFnZS1rb3diZHVwcS5qcGc.jpg",
                    "https://images.rawpixel.com/editor_1024/cHJpdmF0ZS9sci9pbWFnZXMvd2Vic2l0ZS8yMDIzLTA0L2JzNjg4LWltYWdlLmpwZw.jpg",
                    "https://pd.w.org/2025/01/31667956b96946b35.74104785-2048x1365.jpg"
            );
            case "fash-wat-chrono" -> urls(
                    "https://live.staticflickr.com/3324/3575148810_88749bec8e_b.jpg",
                    "https://live.staticflickr.com/4333/36113831143_331f4d9371_b.jpg",
                    "https://images.rawpixel.com/editor_1024/cHJpdmF0ZS9sci9pbWFnZXMvd2Vic2l0ZS8yMDIzLTA0L2JzMTI5OS1pbWFnZS5qcGc.jpg"
            );
            default -> kindFallbackUrls(productKind);
        };
    }

    private List<String> variantImageUrls(String productSku, ProductKind productKind, String visualDescriptor) {
        return switch (productSku) {
            case "tech-phn-novax" -> switch (visualDescriptor) {
                case "black" -> urls(
                        "https://pd.w.org/2026/06/2086a2ac94c64ec74.45914128-1536x2048.jpg",
                        "https://live.staticflickr.com/3849/18998496565_d7844460e3.jpg",
                        "https://live.staticflickr.com/5696/22389367255_6ba8a17d76_b.jpg"
                );
                case "silver" -> urls(
                        "https://live.staticflickr.com/3849/18998496565_d7844460e3.jpg",
                        "https://pd.w.org/2026/06/2086a2ac94c64ec74.45914128-1536x2048.jpg",
                        "https://live.staticflickr.com/5696/22389367255_6ba8a17d76_b.jpg"
                );
                case "ocean-blue" -> urls(
                        "https://live.staticflickr.com/5696/22389367255_6ba8a17d76_b.jpg",
                        "https://pd.w.org/2026/06/2086a2ac94c64ec74.45914128-1536x2048.jpg",
                        "https://live.staticflickr.com/3849/18998496565_d7844460e3.jpg"
                );
                default -> List.of();
            };
            case "tech-lap-wb14" -> switch (visualDescriptor) {
                case "silver" -> urls(
                        "https://cdn.stocksnap.io/img-thumbs/960w/FG432B2OT4.jpg",
                        "https://cdn.stocksnap.io/img-thumbs/960w/8PUO9PFLV2.jpg",
                        "https://live.staticflickr.com/2307/2193891309_516400c479_b.jpg"
                );
                case "space-gray" -> urls(
                        "https://live.staticflickr.com/2307/2193891309_516400c479_b.jpg",
                        "https://cdn.stocksnap.io/img-thumbs/960w/8PUO9PFLV2.jpg",
                        "https://cdn.stocksnap.io/img-thumbs/960w/FG432B2OT4.jpg"
                );
                default -> List.of();
            };
            case "tech-aud-spm" -> switch (visualDescriptor) {
                case "white" -> urls(
                        "https://images.rawpixel.com/editor_1024/czNmcy1wcml2YXRlL3Jhd3BpeGVsX2ltYWdlcy93ZWJzaXRlX2NvbnRlbnQvbHIvaXMxMzIwMy1pbWFnZS1rd3lzZTFnci5qcGc.jpg",
                        "https://images.rawpixel.com/editor_1024/czNmcy1wcml2YXRlL3Jhd3BpeGVsX2ltYWdlcy93ZWJzaXRlX2NvbnRlbnQvbHIvcHg4NjA2ODUtaW1hZ2Uta3d2dXZ5d24uanBn.jpg",
                        "https://live.staticflickr.com/5763/22921682035_334b6161c9_b.jpg"
                );
                case "black" -> urls(
                        "https://live.staticflickr.com/5763/22921682035_334b6161c9_b.jpg",
                        "https://images.rawpixel.com/editor_1024/czNmcy1wcml2YXRlL3Jhd3BpeGVsX2ltYWdlcy93ZWJzaXRlX2NvbnRlbnQvbHIvaXMxMzIwMy1pbWFnZS1rd3lzZTFnci5qcGc.jpg",
                        "https://images.rawpixel.com/editor_1024/czNmcy1wcml2YXRlL3Jhd3BpeGVsX2ltYWdlcy93ZWJzaXRlX2NvbnRlbnQvbHIvcHg4NjA2ODUtaW1hZ2Uta3d2dXZ5d24uanBn.jpg"
                );
                case "forest-green" -> urls(
                        "https://live.staticflickr.com/5763/22921682035_334b6161c9_b.jpg",
                        "https://images.rawpixel.com/editor_1024/czNmcy1wcml2YXRlL3Jhd3BpeGVsX2ltYWdlcy93ZWJzaXRlX2NvbnRlbnQvbHIvcHg4NjA2ODUtaW1hZ2Uta3d2dXZ5d24uanBn.jpg",
                        "https://images.rawpixel.com/editor_1024/czNmcy1wcml2YXRlL3Jhd3BpeGVsX2ltYWdlcy93ZWJzaXRlX2NvbnRlbnQvbHIvaXMxMzIwMy1pbWFnZS1rd3lzZTFnci5qcGc.jpg"
                );
                default -> List.of();
            };
            case "home-sof-cloud" -> switch (visualDescriptor) {
                case "linen-blend", "sand" -> urls(
                        "https://live.staticflickr.com/2829/10798290264_ea1fd4754d_b.jpg",
                        "https://live.staticflickr.com/4656/39953731391_ca612a812a_b.jpg",
                        "https://live.staticflickr.com/65535/48089881637_86af42f066_b.jpg"
                );
                case "performance-velvet", "graphite" -> urls(
                        "https://live.staticflickr.com/4656/39953731391_ca612a812a_b.jpg",
                        "https://live.staticflickr.com/2829/10798290264_ea1fd4754d_b.jpg",
                        "https://live.staticflickr.com/65535/48089881637_86af42f066_b.jpg"
                );
                case "soft-leather", "olive" -> urls(
                        "https://live.staticflickr.com/65535/48089881637_86af42f066_b.jpg",
                        "https://live.staticflickr.com/4656/39953731391_ca612a812a_b.jpg",
                        "https://live.staticflickr.com/2829/10798290264_ea1fd4754d_b.jpg"
                );
                default -> List.of();
            };
            case "home-din-nord" -> switch (visualDescriptor) {
                case "oak", "matte" -> urls(
                        "https://upload.wikimedia.org/wikipedia/commons/8/8c/Mahogany_dining_table_attributed_to_Duncan_Phyfe%2C_c._1815%2C_Dayton_Art_Institute.JPG",
                        "https://upload.wikimedia.org/wikipedia/commons/b/be/Drop-leaf_dining_table_MET_DP104721.jpg",
                        "https://upload.wikimedia.org/wikipedia/commons/0/01/William_H._Edwards%2C_Dining_Table%2C_1935-1942%2C_NGA_17904.jpg"
                );
                case "walnut", "oiled" -> urls(
                        "https://upload.wikimedia.org/wikipedia/commons/0/01/William_H._Edwards%2C_Dining_Table%2C_1935-1942%2C_NGA_17904.jpg",
                        "https://upload.wikimedia.org/wikipedia/commons/8/8c/Mahogany_dining_table_attributed_to_Duncan_Phyfe%2C_c._1815%2C_Dayton_Art_Institute.JPG",
                        "https://upload.wikimedia.org/wikipedia/commons/b/be/Drop-leaf_dining_table_MET_DP104721.jpg"
                );
                case "ash" -> urls(
                        "https://upload.wikimedia.org/wikipedia/commons/b/be/Drop-leaf_dining_table_MET_DP104721.jpg",
                        "https://upload.wikimedia.org/wikipedia/commons/8/8c/Mahogany_dining_table_attributed_to_Duncan_Phyfe%2C_c._1815%2C_Dayton_Art_Institute.JPG",
                        "https://upload.wikimedia.org/wikipedia/commons/0/01/William_H._Edwards%2C_Dining_Table%2C_1935-1942%2C_NGA_17904.jpg"
                );
                default -> List.of();
            };
            case "home-kit-barista" -> switch (visualDescriptor) {
                case "matte-black" -> urls(
                        "https://live.staticflickr.com/41/87881542_7ad8398828_b.jpg",
                        "https://live.staticflickr.com/1477/25299515990_e4e6706627_b.jpg",
                        "https://live.staticflickr.com/830/27715495688_cec57a9141_b.jpg"
                );
                case "cream-white" -> urls(
                        "https://live.staticflickr.com/1477/25299515990_e4e6706627_b.jpg",
                        "https://live.staticflickr.com/830/27715495688_cec57a9141_b.jpg",
                        "https://live.staticflickr.com/41/87881542_7ad8398828_b.jpg"
                );
                default -> List.of();
            };
            case "out-bik-tbcrb" -> switch (visualDescriptor) {
                case "stealth-black" -> urls(
                        "https://live.staticflickr.com/3839/33265683642_7787075506_b.jpg",
                        "https://live.staticflickr.com/771/33421391135_319aac8b07_b.jpg",
                        "https://live.staticflickr.com/3699/33421401745_b33002e0a2_b.jpg"
                );
                case "canyon-red" -> urls(
                        "https://live.staticflickr.com/3699/33421401745_b33002e0a2_b.jpg",
                        "https://live.staticflickr.com/771/33421391135_319aac8b07_b.jpg",
                        "https://live.staticflickr.com/3839/33265683642_7787075506_b.jpg"
                );
                case "desert-sand" -> urls(
                        "https://live.staticflickr.com/771/33421391135_319aac8b07_b.jpg",
                        "https://live.staticflickr.com/3839/33265683642_7787075506_b.jpg",
                        "https://live.staticflickr.com/3699/33421401745_b33002e0a2_b.jpg"
                );
                default -> List.of();
            };
            case "out-cmp-summit" -> switch (visualDescriptor) {
                case "alpine-orange" -> urls(
                        "https://cdn.stocksnap.io/img-thumbs/960w/177F2EBDDC.jpg",
                        "https://cdn.stocksnap.io/img-thumbs/960w/PTERNSNX0F.jpg",
                        "https://cdn.stocksnap.io/img-thumbs/960w/OCXLY7U3OG.jpg"
                );
                case "forest-green" -> urls(
                        "https://cdn.stocksnap.io/img-thumbs/960w/OCXLY7U3OG.jpg",
                        "https://cdn.stocksnap.io/img-thumbs/960w/PTERNSNX0F.jpg",
                        "https://cdn.stocksnap.io/img-thumbs/960w/177F2EBDDC.jpg"
                );
                default -> List.of();
            };
            case "out-pad-riverrun" -> switch (visualDescriptor) {
                case "blue-wave" -> urls(
                        "https://pd.w.org/2026/06/3436a255f2bcc5d77.22185172-2048x1536.jpg",
                        "https://live.staticflickr.com/8822/17463418025_2042eda5df_b.jpg",
                        "https://live.staticflickr.com/5467/16840775384_aeb72d4867_b.jpg"
                );
                case "sun-yellow" -> urls(
                        "https://images.rawpixel.com/editor_1024/czNmcy1wcml2YXRlL3Jhd3BpeGVsX2ltYWdlcy93ZWJzaXRlX2NvbnRlbnQvbHIvdXB3azYyMTUzNDQ3LXdpa2ltZWRpYS1pbWFnZS1rb3dhc3c1cS5qcGc.jpg",
                        "https://pd.w.org/2026/07/986a4aecb9a37a75.00506266-1536x2048.jpg",
                        "https://pd.w.org/2024/07/43566a38770c24c07.70364088-2048x1536.jpg"
                );
                case "rescue-red" -> urls(
                        "https://live.staticflickr.com/4121/4802920219_2b54653d93_b.jpg",
                        "https://images.rawpixel.com/editor_1024/czNmcy1wcml2YXRlL3Jhd3BpeGVsX2ltYWdlcy93ZWJzaXRlX2NvbnRlbnQvbHIvdXB3azYyMjEyOTgzLXdpa2ltZWRpYS1pbWFnZS1rb3dzNWppeS5qcGc.jpg",
                        "https://upload.wikimedia.org/wikipedia/commons/f/f6/Red_kayaks_on_a_wall_%28Unsplash%29.jpg"
                );
                default -> List.of();
            };
            case "fash-snk-aerost" -> switch (visualDescriptor) {
                case "black" -> urls(
                        "https://cdn.stocksnap.io/img-thumbs/960w/645GI8G1W4.jpg",
                        "https://upload.wikimedia.org/wikipedia/commons/f/f5/Running_shoes_display.JPG",
                        "https://live.staticflickr.com/4354/36218260284_d07ee0f055_b.jpg"
                );
                case "white" -> urls(
                        "https://upload.wikimedia.org/wikipedia/commons/f/f5/Running_shoes_display.JPG",
                        "https://cdn.stocksnap.io/img-thumbs/960w/645GI8G1W4.jpg",
                        "https://live.staticflickr.com/4354/36218260284_d07ee0f055_b.jpg"
                );
                case "navy" -> urls(
                        "https://live.staticflickr.com/4354/36218260284_d07ee0f055_b.jpg",
                        "https://cdn.stocksnap.io/img-thumbs/960w/645GI8G1W4.jpg",
                        "https://upload.wikimedia.org/wikipedia/commons/f/f5/Running_shoes_display.JPG"
                );
                default -> List.of();
            };
            case "fash-out-alpine" -> switch (visualDescriptor) {
                case "black" -> urls(
                        "https://images.rawpixel.com/editor_1024/czNmcy1wcml2YXRlL3Jhd3BpeGVsX2ltYWdlcy93ZWJzaXRlX2NvbnRlbnQvbHIvdXB3azYxNzQxOTQzLXdpa2ltZWRpYS1pbWFnZS1rb3diZHVwcS5qcGc.jpg",
                        "https://pd.w.org/2025/01/238678505e1aac283.14469821-1365x2048.jpg",
                        "https://images.rawpixel.com/editor_1024/cHJpdmF0ZS9sci9pbWFnZXMvd2Vic2l0ZS8yMDIzLTA0L2JzNjg4LWltYWdlLmpwZw.jpg"
                );
                case "moss" -> urls(
                        "https://pd.w.org/2025/01/31667956b96946b35.74104785-2048x1365.jpg",
                        "https://images.rawpixel.com/editor_1024/czNmcy1wcml2YXRlL3Jhd3BpeGVsX2ltYWdlcy93ZWJzaXRlX2NvbnRlbnQvbHIvdXB3azYxNzQxOTQzLXdpa2ltZWRpYS1pbWFnZS1rb3diZHVwcS5qcGc.jpg",
                        "https://images.rawpixel.com/editor_1024/cHJpdmF0ZS9sci9pbWFnZXMvd2Vic2l0ZS8yMDIzLTA0L2JzNjg4LWltYWdlLmpwZw.jpg"
                );
                case "signal-orange" -> urls(
                        "https://images.rawpixel.com/editor_1024/cHJpdmF0ZS9sci9pbWFnZXMvd2Vic2l0ZS8yMDIzLTA0L2JzNjg4LWltYWdlLmpwZw.jpg",
                        "https://pd.w.org/2025/01/31667956b96946b35.74104785-2048x1365.jpg",
                        "https://images.rawpixel.com/editor_1024/czNmcy1wcml2YXRlL3Jhd3BpeGVsX2ltYWdlcy93ZWJzaXRlX2NvbnRlbnQvbHIvdXB3azYxNzQxOTQzLXdpa2ltZWRpYS1pbWFnZS1rb3diZHVwcS5qcGc.jpg"
                );
                default -> List.of();
            };
            case "fash-wat-chrono" -> switch (visualDescriptor) {
                case "silicone", "black" -> urls(
                        "https://live.staticflickr.com/3324/3575148810_88749bec8e_b.jpg",
                        "https://live.staticflickr.com/4333/36113831143_331f4d9371_b.jpg",
                        "https://upload.wikimedia.org/wikipedia/commons/6/6f/Mechanics_movement_feinmechanik_wrist_watch_clock_automatic_gmt_master_gmt-932709.jpg%21d.jpg"
                );
                case "leather", "ivory" -> urls(
                        "https://images.rawpixel.com/editor_1024/cHJpdmF0ZS9sci9pbWFnZXMvd2Vic2l0ZS8yMDIzLTA0L2JzMTI5OS1pbWFnZS5qcGc.jpg",
                        "https://live.staticflickr.com/4333/36113831143_331f4d9371_b.jpg",
                        "https://live.staticflickr.com/3324/3575148810_88749bec8e_b.jpg"
                );
                case "steel", "blue" -> urls(
                        "https://upload.wikimedia.org/wikipedia/commons/6/6f/Mechanics_movement_feinmechanik_wrist_watch_clock_automatic_gmt_master_gmt-932709.jpg%21d.jpg",
                        "https://live.staticflickr.com/3324/3575148810_88749bec8e_b.jpg",
                        "https://live.staticflickr.com/4333/36113831143_331f4d9371_b.jpg"
                );
                default -> List.of();
            };
            default -> descriptorFallbackUrls(productKind, visualDescriptor);
        };
    }

    private List<String> kindFallbackUrls(ProductKind productKind) {
        return switch (productKind) {
            case SMARTPHONE -> urls(
                    "https://pd.w.org/2026/06/2086a2ac94c64ec74.45914128-1536x2048.jpg",
                    "https://live.staticflickr.com/3849/18998496565_d7844460e3.jpg",
                    "https://live.staticflickr.com/5696/22389367255_6ba8a17d76_b.jpg"
            );
            case LAPTOP -> urls(
                    "https://cdn.stocksnap.io/img-thumbs/960w/FG432B2OT4.jpg",
                    "https://cdn.stocksnap.io/img-thumbs/960w/8PUO9PFLV2.jpg",
                    "https://live.staticflickr.com/2307/2193891309_516400c479_b.jpg"
            );
            case HEADPHONES -> urls(
                    "https://images.rawpixel.com/editor_1024/czNmcy1wcml2YXRlL3Jhd3BpeGVsX2ltYWdlcy93ZWJzaXRlX2NvbnRlbnQvbHIvaXMxMzIwMy1pbWFnZS1rd3lzZTFnci5qcGc.jpg",
                    "https://images.rawpixel.com/editor_1024/czNmcy1wcml2YXRlL3Jhd3BpeGVsX2ltYWdlcy93ZWJzaXRlX2NvbnRlbnQvbHIvcHg4NjA2ODUtaW1hZ2Uta3d2dXZ5d24uanBn.jpg",
                    "https://live.staticflickr.com/5763/22921682035_334b6161c9_b.jpg"
            );
            case SOFA -> urls(
                    "https://live.staticflickr.com/65535/48089881637_86af42f066_b.jpg",
                    "https://live.staticflickr.com/4656/39953731391_ca612a812a_b.jpg",
                    "https://live.staticflickr.com/2829/10798290264_ea1fd4754d_b.jpg"
            );
            case TABLE -> urls(
                    "https://upload.wikimedia.org/wikipedia/commons/8/8c/Mahogany_dining_table_attributed_to_Duncan_Phyfe%2C_c._1815%2C_Dayton_Art_Institute.JPG",
                    "https://upload.wikimedia.org/wikipedia/commons/b/be/Drop-leaf_dining_table_MET_DP104721.jpg",
                    "https://upload.wikimedia.org/wikipedia/commons/0/01/William_H._Edwards%2C_Dining_Table%2C_1935-1942%2C_NGA_17904.jpg"
            );
            case COFFEE_MACHINE -> urls(
                    "https://live.staticflickr.com/41/87881542_7ad8398828_b.jpg",
                    "https://live.staticflickr.com/1477/25299515990_e4e6706627_b.jpg",
                    "https://live.staticflickr.com/830/27715495688_cec57a9141_b.jpg"
            );
            case BIKE -> urls(
                    "https://live.staticflickr.com/771/33421391135_319aac8b07_b.jpg",
                    "https://live.staticflickr.com/3839/33265683642_7787075506_b.jpg",
                    "https://live.staticflickr.com/3699/33421401745_b33002e0a2_b.jpg"
            );
            case TENT -> urls(
                    "https://cdn.stocksnap.io/img-thumbs/960w/177F2EBDDC.jpg",
                    "https://cdn.stocksnap.io/img-thumbs/960w/OCXLY7U3OG.jpg",
                    "https://cdn.stocksnap.io/img-thumbs/960w/PTERNSNX0F.jpg"
            );
            case KAYAK -> urls(
                    "https://live.staticflickr.com/895/41432798741_23875e1597_b.jpg",
                    "https://live.staticflickr.com/811/41432796971_87f5035ab8_b.jpg",
                    "https://live.staticflickr.com/3739/11640366094_31bbf639ca_b.jpg"
            );
            case SHOES -> urls(
                    "https://live.staticflickr.com/4354/36218260284_d07ee0f055_b.jpg",
                    "https://cdn.stocksnap.io/img-thumbs/960w/645GI8G1W4.jpg",
                    "https://upload.wikimedia.org/wikipedia/commons/f/f5/Running_shoes_display.JPG"
            );
            case JACKET -> urls(
                    "https://images.rawpixel.com/editor_1024/czNmcy1wcml2YXRlL3Jhd3BpeGVsX2ltYWdlcy93ZWJzaXRlX2NvbnRlbnQvbHIvdXB3azYxNzQxOTQzLXdpa2ltZWRpYS1pbWFnZS1rb3diZHVwcS5qcGc.jpg",
                    "https://images.rawpixel.com/editor_1024/cHJpdmF0ZS9sci9pbWFnZXMvd2Vic2l0ZS8yMDIzLTA0L2JzNjg4LWltYWdlLmpwZw.jpg",
                    "https://pd.w.org/2025/01/31667956b96946b35.74104785-2048x1365.jpg"
            );
            case WATCH -> urls(
                    "https://live.staticflickr.com/3324/3575148810_88749bec8e_b.jpg",
                    "https://live.staticflickr.com/4333/36113831143_331f4d9371_b.jpg",
                    "https://images.rawpixel.com/editor_1024/cHJpdmF0ZS9sci9pbWFnZXMvd2Vic2l0ZS8yMDIzLTA0L2JzMTI5OS1pbWFnZS5qcGc.jpg"
            );
            case GENERIC -> emergencyFallbackUrls();
        };
    }

    private List<String> descriptorFallbackUrls(ProductKind productKind, String visualDescriptor) {
        return switch (productKind) {
            case SMARTPHONE, LAPTOP, HEADPHONES, SOFA, TABLE, COFFEE_MACHINE, BIKE, TENT, KAYAK, SHOES, JACKET, WATCH ->
                    kindFallbackUrls(productKind);
            case GENERIC -> emergencyFallbackUrls();
        };
    }

    private List<String> emergencyFallbackUrls() {
        return urls(
                "https://pd.w.org/2026/06/2086a2ac94c64ec74.45914128-1536x2048.jpg",
                "https://cdn.stocksnap.io/img-thumbs/960w/FG432B2OT4.jpg",
                "https://cdn.stocksnap.io/img-thumbs/960w/645GI8G1W4.jpg"
        );
    }

    private List<String> urls(String... values) {
        return List.of(values);
    }

    private ProductKind productKind(String productSku, String productName) {
        String normalizedSku = sanitizeKeyPart(productSku);
        String normalizedName = sanitizeKeyPart(productName);
        if (normalizedSku.startsWith("tech-phn") || normalizedName.contains("nova")) {
            return ProductKind.SMARTPHONE;
        }
        if (normalizedSku.startsWith("tech-lap") || normalizedName.contains("workbook")) {
            return ProductKind.LAPTOP;
        }
        if (normalizedSku.startsWith("tech-aud") || normalizedName.contains("studio-max")) {
            return ProductKind.HEADPHONES;
        }
        if (normalizedName.contains("sofa")) {
            return ProductKind.SOFA;
        }
        if (normalizedName.contains("table")) {
            return ProductKind.TABLE;
        }
        if (normalizedName.contains("coffee-station") || normalizedName.contains("coffee")) {
            return ProductKind.COFFEE_MACHINE;
        }
        if (normalizedName.contains("carbon-gx") || normalizedName.contains("bike")) {
            return ProductKind.BIKE;
        }
        if (normalizedName.contains("tent")) {
            return ProductKind.TENT;
        }
        if (normalizedName.contains("kayak")) {
            return ProductKind.KAYAK;
        }
        if (normalizedName.contains("runner") || normalizedName.contains("shoe")) {
            return ProductKind.SHOES;
        }
        if (normalizedName.contains("jacket")) {
            return ProductKind.JACKET;
        }
        if (normalizedName.contains("watch")) {
            return ProductKind.WATCH;
        }
        return ProductKind.GENERIC;
    }

    private String visualDescriptor(Map<String, Object> options) {
        for (String key : List.of("color", "finish", "fabric", "strap", "dial", "upholstery", "shell", "shellColor", "material")) {
            Object value = options.get(key);
            if (value instanceof String text && !text.isBlank()) {
                return text;
            }
        }
        return null;
    }

    private String sanitizeKeyPart(String value) {
        String normalized = String.valueOf(value == null ? "" : value).trim().toLowerCase(Locale.ROOT);
        normalized = normalized.replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return normalized.isBlank() ? "item" : normalized;
    }

    private enum ProductKind {
        SMARTPHONE,
        LAPTOP,
        HEADPHONES,
        SOFA,
        TABLE,
        COFFEE_MACHINE,
        BIKE,
        TENT,
        KAYAK,
        SHOES,
        JACKET,
        WATCH,
        GENERIC
    }
}

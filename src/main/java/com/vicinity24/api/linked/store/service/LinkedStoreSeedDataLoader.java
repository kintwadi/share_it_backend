package com.vicinity24.api.linked.store.service;

import com.vicinity24.api.core.storage.StorageManager;
import com.vicinity24.api.linked.store.entity.Store;
import com.vicinity24.api.linked.store.entity.StoreCategory;
import com.vicinity24.api.linked.store.entity.StoreProduct;
import com.vicinity24.api.linked.store.entity.StoreProductVariant;
import com.vicinity24.api.linked.store.repository.StoreCatalogCategoryRepository;
import com.vicinity24.api.linked.store.repository.StoreCatalogProductRepository;
import com.vicinity24.api.linked.store.repository.StoreCatalogProductVariantRepository;
import com.vicinity24.api.linked.store.repository.StoreCatalogStoreRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class LinkedStoreSeedDataLoader implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(LinkedStoreSeedDataLoader.class);

    private final StoreCatalogStoreRepository storeRepository;
    private final StoreCatalogCategoryRepository categoryRepository;
    private final StoreCatalogProductRepository productRepository;
    private final StoreCatalogProductVariantRepository variantRepository;
    private final StorageManager storageManager;
    private final LinkedStoreRealImageService realImageService;

    public LinkedStoreSeedDataLoader(
            StoreCatalogStoreRepository storeRepository,
            StoreCatalogCategoryRepository categoryRepository,
            StoreCatalogProductRepository productRepository,
            StoreCatalogProductVariantRepository variantRepository,
            StorageManager storageManager,
            LinkedStoreRealImageService realImageService
    ) {
        this.storeRepository = storeRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.variantRepository = variantRepository;
        this.storageManager = storageManager;
        this.realImageService = realImageService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (StoreSeed seed : seedDefinitions()) {
            seedStore(seed);
        }

        log.info(
                "Linked store seed data loaded: stores={}, categories={}, products={}, variants={}",
                storeRepository.count(),
                categoryRepository.count(),
                productRepository.count(),
                variantRepository.count()
        );
    }

    private void seedStore(StoreSeed seed) {
        Store store = storeRepository.findBySlugIgnoreCase(seed.slug())
                .orElseGet(Store::new);
        store.setName(seed.name());
        store.setSlug(seed.slug());
        store = storeRepository.save(store);
        store.setBannerImageUrl(realImageService.resolveStoreBanner(store));
        store = storeRepository.save(store);

        Map<String, StoreCategory> categoriesBySlug = new LinkedHashMap<>();
        for (CategorySeed categorySeed : seed.categories()) {
            StoreCategory parent = categorySeed.parentSlug() == null ? null : categoriesBySlug.get(categorySeed.parentSlug());
            StoreCategory category = categoryRepository.findByStoreIdAndSlugIgnoreCase(store.getId(), categorySeed.slug())
                    .orElseGet(StoreCategory::new);
            category.setStore(store);
            category.setParent(parent);
            category.setName(categorySeed.name());
            category.setSlug(categorySeed.slug());
            Map<String, Object> attributeSchema = copyMap(categorySeed.attributeSchema());
            String parentName = parent == null ? store.getName() : parent.getName();
            attributeSchema.put("bannerImageUrl", realImageService.resolveCategoryBanner(store, categorySeed.slug(), categorySeed.name(), parentName));
            category.setAttributeSchema(attributeSchema);
            category = categoryRepository.save(category);
            categoriesBySlug.put(categorySeed.slug(), category);
        }

        for (ProductSeed productSeed : seed.products()) {
            StoreCategory category = categoriesBySlug.get(productSeed.categorySlug());
            StoreProduct product = productRepository.findByStoreIdAndSkuIgnoreCase(store.getId(), productSeed.sku())
                    .orElseGet(StoreProduct::new);
            product.setStore(store);
            product.setCategory(category);
            product.setSku(productSeed.sku());
            product.setName(productSeed.name());
            product.setDescription(productSeed.description());
            product.setBasePrice(productSeed.basePrice());
            product.setCurrency(productSeed.currency());
            Map<String, Object> properties = copyMap(productSeed.properties());
            properties.put("images", realImageService.resolveProductImages(store, category, productSeed.sku(), productSeed.name(), properties));
            product.setProperties(properties);
            product.setActive(productSeed.active());
            product = productRepository.save(product);

            for (VariantSeed variantSeed : productSeed.variants()) {
                StoreProductVariant variant = variantRepository.findByStoreIdAndSkuIgnoreCase(store.getId(), variantSeed.sku())
                        .orElseGet(StoreProductVariant::new);
                variant.setStore(store);
                variant.setProduct(product);
                variant.setSku(variantSeed.sku());
                variant.setPrice(variantSeed.price());
                variant.setStock(variantSeed.stock());
                Map<String, Object> variantOptions = copyMap(variantSeed.options());
                variantOptions.put("images", realImageService.resolveVariantImages(
                        store,
                        category,
                        productSeed.sku(),
                        productSeed.name(),
                        variantSeed.sku(),
                        variantOptions
                ));
                variant.setOptions(variantOptions);
                variant.setActive(variantSeed.active());
                variantRepository.save(variant);
            }
        }
    }

    private List<StoreSeed> seedDefinitions() {
        return List.of(
                technologyStore(),
                homeStore(),
                outdoorStore(),
                fashionStore()
        );
    }

    private StoreSeed technologyStore() {
        return new StoreSeed(
                "Tech Hub Europe",
                "tech-hub-europe",
                List.of(
                        new CategorySeed("electronics", "Electronics", null, map(
                                "department", "electronics",
                                "audience", List.of("consumer", "professional"),
                                "filterGroups", List.of("brand", "price", "availability")
                        )),
                        new CategorySeed("smartphones", "Smartphones", "electronics", map(
                                "attributes", List.of("brand", "screen", "batteryMah", "cameraSystem", "storage", "connectivity", "color"),
                                "variantAxes", List.of("color", "storage", "connectivity"),
                                "warrantyYears", List.of(2, 3)
                        )),
                        new CategorySeed("laptops", "Laptops", "electronics", map(
                                "attributes", List.of("brand", "processor", "ram", "storage", "display", "keyboardLayout", "finish"),
                                "variantAxes", List.of("finish", "ram", "storage", "keyboardLayout"),
                                "businessReady", true
                        )),
                        new CategorySeed("audio", "Audio", "electronics", map(
                                "attributes", List.of("brand", "noiseCancelling", "driverSize", "batteryHours", "bundle", "warranty"),
                                "variantAxes", List.of("color", "bundle", "warranty")
                        ))
                ),
                List.of(
                        new ProductSeed(
                                "TECH-PHN-NOVAX",
                                "Orion Nova X",
                                "Flagship smartphone with all-day battery life, AI camera modes, and premium AMOLED display.",
                                money("899.00"),
                                "EUR",
                                "smartphones",
                                productProperties("Orion Nova X",
                                        "brand", "Orion",
                                        "model", "Nova X",
                                        "screen", "6.7-inch AMOLED 120Hz",
                                        "batteryMah", 5200,
                                        "cameraSystem", List.of("50MP main", "12MP ultra-wide", "10MP telephoto"),
                                        "operatingSystem", "Android 15",
                                        "waterResistance", "IP68"
                                ),
                                true,
                                variants(
                                        "TECH-PHN-NOVAX",
                                        money("899.00"),
                                        18,
                                        axis("color",
                                                choice("Black", "BLK", "0.00", 2),
                                                choice("Silver", "SLV", "20.00", 1),
                                                choice("Ocean Blue", "BLU", "15.00", 0)
                                        ),
                                        axis("storage",
                                                choice("128GB", "128", "0.00", 3),
                                                choice("256GB", "256", "100.00", 1),
                                                choice("512GB", "512", "220.00", -1)
                                        ),
                                        axis("connectivity",
                                                choice("5G", "5G", "0.00", 1),
                                                choice("eSIM + 5G", "ESIM", "40.00", -1)
                                        )
                                )
                        ),
                        new ProductSeed(
                                "TECH-LAP-WB14",
                                "Vertex WorkBook Pro 14",
                                "Lightweight professional laptop with high-resolution display, long battery life, and creator-grade performance.",
                                money("1499.00"),
                                "EUR",
                                "laptops",
                                productProperties("Vertex WorkBook Pro 14",
                                        "brand", "Vertex",
                                        "model", "WorkBook Pro 14",
                                        "processor", "Intel Core Ultra 7",
                                        "display", "14.2-inch 3K IPS",
                                        "graphics", "Integrated Arc Graphics",
                                        "batteryHours", 15,
                                        "ports", List.of("Thunderbolt 4", "USB-C", "HDMI", "SD Card")
                                ),
                                true,
                                variants(
                                        "TECH-LAP-WB14",
                                        money("1499.00"),
                                        9,
                                        axis("finish",
                                                choice("Silver", "SIL", "0.00", 1),
                                                choice("Space Gray", "SGR", "30.00", 0)
                                        ),
                                        axis("ram",
                                                choice("16GB", "16", "0.00", 2),
                                                choice("32GB", "32", "220.00", 0)
                                        ),
                                        axis("storage",
                                                choice("512GB SSD", "512", "0.00", 1),
                                                choice("1TB SSD", "1TB", "180.00", 0),
                                                choice("2TB SSD", "2TB", "420.00", -1)
                                        ),
                                        axis("keyboardLayout",
                                                choice("US", "US", "0.00", 1),
                                                choice("DE", "DE", "0.00", 0)
                                        )
                                )
                        ),
                        new ProductSeed(
                                "TECH-AUD-SPM",
                                "Pulse Studio Max",
                                "Premium over-ear noise-cancelling headphones tuned for travel, focus, and studio listening.",
                                money("329.00"),
                                "EUR",
                                "audio",
                                productProperties("Pulse Studio Max",
                                        "brand", "Pulse",
                                        "model", "Studio Max",
                                        "batteryHours", 40,
                                        "noiseCancelling", true,
                                        "bluetooth", "5.4",
                                        "driverSizeMm", 40
                                ),
                                true,
                                variants(
                                        "TECH-AUD-SPM",
                                        money("329.00"),
                                        14,
                                        axis("color",
                                                choice("Black", "BLK", "0.00", 2),
                                                choice("White", "WHT", "10.00", 1),
                                                choice("Forest Green", "GRN", "15.00", 0)
                                        ),
                                        axis("bundle",
                                                choice("Standard", "STD", "0.00", 1),
                                                choice("Travel Case", "TRV", "35.00", 0)
                                        ),
                                        axis("warranty",
                                                choice("2 Years", "2Y", "0.00", 0),
                                                choice("3 Years", "3Y", "25.00", -1)
                                        )
                                )
                        )
                )
        );
    }

    private StoreSeed homeStore() {
        return new StoreSeed(
                "Urban Home Living",
                "urban-home-living",
                List.of(
                        new CategorySeed("home", "Home Living", null, map(
                                "department", "home",
                                "filterGroups", List.of("room", "style", "material", "availability")
                        )),
                        new CategorySeed("sofas", "Sofas", "home", map(
                                "attributes", List.of("collection", "fabric", "seatCount", "orientation", "color"),
                                "variantAxes", List.of("fabric", "seatCount", "color")
                        )),
                        new CategorySeed("dining", "Dining", "home", map(
                                "attributes", List.of("material", "size", "finish", "extension", "shape"),
                                "variantAxes", List.of("size", "material", "finish")
                        )),
                        new CategorySeed("kitchen-appliances", "Kitchen Appliances", "home", map(
                                "attributes", List.of("voltage", "bundle", "color", "tankCapacity", "grinder"),
                                "variantAxes", List.of("color", "voltage", "bundle")
                        ))
                ),
                List.of(
                        new ProductSeed(
                                "HOME-SOF-CLOUD",
                                "Cloud Modular Sofa",
                                "Deep-seat modular sofa designed for family lounges, flexible layouts, and premium comfort finishes.",
                                money("1299.00"),
                                "EUR",
                                "sofas",
                                productProperties("Cloud Modular Sofa",
                                        "brand", "Loft & Line",
                                        "collection", "Cloud",
                                        "seatDepthCm", 68,
                                        "frameMaterial", "Kiln-dried hardwood",
                                        "assembly", "Tool-free connector clips"
                                ),
                                true,
                                variants(
                                        "HOME-SOF-CLOUD",
                                        money("1299.00"),
                                        4,
                                        axis("fabric",
                                                choice("Linen Blend", "LIN", "0.00", 1),
                                                choice("Performance Velvet", "VEL", "180.00", 0),
                                                choice("Soft Leather", "LTH", "420.00", -1)
                                        ),
                                        axis("seatCount",
                                                choice("2-Seater", "2S", "0.00", 1),
                                                choice("3-Seater", "3S", "260.00", 0),
                                                choice("4-Seater", "4S", "520.00", -1)
                                        ),
                                        axis("color",
                                                choice("Sand", "SND", "0.00", 1),
                                                choice("Graphite", "GPH", "25.00", 0),
                                                choice("Olive", "OLV", "40.00", 0)
                                        )
                                )
                        ),
                        new ProductSeed(
                                "HOME-DIN-NORD",
                                "Nordic Extend Table",
                                "Extendable dining table with refined joinery, optional large hosting sizes, and solid wood character.",
                                money("899.00"),
                                "EUR",
                                "dining",
                                productProperties("Nordic Extend Table",
                                        "brand", "Nord Atelier",
                                        "collection", "Nordic Extend",
                                        "shape", "Rectangular",
                                        "extensionLeaf", true,
                                        "care", "Food-safe hardwax oil"
                                ),
                                true,
                                variants(
                                        "HOME-DIN-NORD",
                                        money("899.00"),
                                        6,
                                        axis("size",
                                                choice("160cm", "160", "0.00", 1),
                                                choice("200cm", "200", "190.00", 0),
                                                choice("240cm", "240", "390.00", -1)
                                        ),
                                        axis("material",
                                                choice("Oak", "OAK", "0.00", 1),
                                                choice("Walnut", "WAL", "220.00", 0),
                                                choice("Ash", "ASH", "80.00", 0)
                                        ),
                                        axis("finish",
                                                choice("Matte", "MAT", "0.00", 0),
                                                choice("Oiled", "OIL", "60.00", 0)
                                        )
                                )
                        ),
                        new ProductSeed(
                                "HOME-KIT-BARISTA",
                                "Barista Pro Coffee Station",
                                "All-in-one espresso station with integrated grinder, steam wand, and configurable starter bundles.",
                                money("649.00"),
                                "EUR",
                                "kitchen-appliances",
                                productProperties("Barista Pro Coffee Station",
                                        "brand", "Barista Pro",
                                        "pumpPressureBar", 15,
                                        "tankCapacityL", 2.3,
                                        "grinderLevels", 30,
                                        "milkSystem", "Manual steam wand"
                                ),
                                true,
                                variants(
                                        "HOME-KIT-BARISTA",
                                        money("649.00"),
                                        10,
                                        axis("color",
                                                choice("Matte Black", "MBK", "0.00", 1),
                                                choice("Cream White", "CRM", "20.00", 0)
                                        ),
                                        axis("voltage",
                                                choice("EU 220V", "EU", "0.00", 1),
                                                choice("UK 240V", "UK", "0.00", 0)
                                        ),
                                        axis("bundle",
                                                choice("Starter Kit", "ST", "0.00", 1),
                                                choice("Premium Barista Set", "PR", "90.00", 0)
                                        )
                                )
                        )
                )
        );
    }

    private StoreSeed outdoorStore() {
        return new StoreSeed(
                "Active Outdoors Pro",
                "active-outdoors-pro",
                List.of(
                        new CategorySeed("outdoor", "Outdoor & Adventure", null, map(
                                "department", "outdoor",
                                "seasonality", List.of("spring", "summer", "autumn", "winter")
                        )),
                        new CategorySeed("bikes", "Adventure Bikes", "outdoor", map(
                                "attributes", List.of("frame", "wheelSize", "groupset", "travel", "terrain"),
                                "variantAxes", List.of("frame", "color", "groupset")
                        )),
                        new CategorySeed("camping", "Camping", "outdoor", map(
                                "attributes", List.of("capacity", "seasonRating", "package", "weightKg", "color"),
                                "variantAxes", List.of("capacity", "shellColor", "package")
                        )),
                        new CategorySeed("paddling", "Paddling", "outdoor", map(
                                "attributes", List.of("length", "capacityKg", "cockpit", "rudder", "package"),
                                "variantAxes", List.of("length", "color", "package")
                        ))
                ),
                List.of(
                        new ProductSeed(
                                "OUT-BIK-TBCRB",
                                "TrailBlazer Carbon GX",
                                "Fast trail bike built for technical climbs, rough descents, and confident all-day performance.",
                                money("2699.00"),
                                "EUR",
                                "bikes",
                                productProperties("TrailBlazer Carbon GX",
                                        "brand", "TrailBlazer",
                                        "wheelSize", "29-inch",
                                        "frontTravelMm", 140,
                                        "rearTravelMm", 130,
                                        "frameMaterial", "Carbon",
                                        "dropperPost", true
                                ),
                                true,
                                variants(
                                        "OUT-BIK-TBCRB",
                                        money("2699.00"),
                                        3,
                                        axis("frame",
                                                choice("Small", "S", "0.00", 1),
                                                choice("Medium", "M", "0.00", 1),
                                                choice("Large", "L", "0.00", 0),
                                                choice("XL", "XL", "0.00", -1)
                                        ),
                                        axis("color",
                                                choice("Stealth Black", "BLK", "0.00", 0),
                                                choice("Canyon Red", "RED", "60.00", 0),
                                                choice("Desert Sand", "SND", "60.00", -1)
                                        ),
                                        axis("groupset",
                                                choice("SRAM GX", "GX", "0.00", 1),
                                                choice("Shimano XT", "XT", "240.00", 0)
                                        )
                                )
                        ),
                        new ProductSeed(
                                "OUT-CMP-SUMMIT",
                                "Summit Dome Expedition Tent",
                                "Four-season dome tent optimized for alpine weather, group camps, and expedition-grade resilience.",
                                money("499.00"),
                                "EUR",
                                "camping",
                                productProperties("Summit Dome Expedition Tent",
                                        "brand", "Summit Camp",
                                        "seasonRating", "4-season",
                                        "waterproofMm", 4000,
                                        "poleMaterial", "DAC aluminum",
                                        "vestibules", 2
                                ),
                                true,
                                variants(
                                        "OUT-CMP-SUMMIT",
                                        money("499.00"),
                                        7,
                                        axis("capacity",
                                                choice("2 Person", "2P", "0.00", 1),
                                                choice("4 Person", "4P", "180.00", 0),
                                                choice("6 Person", "6P", "340.00", -1)
                                        ),
                                        axis("shellColor",
                                                choice("Alpine Orange", "ALP", "0.00", 0),
                                                choice("Forest Green", "FRS", "0.00", 1)
                                        ),
                                        axis("package",
                                                choice("Standard", "STD", "0.00", 1),
                                                choice("Expedition Kit", "EXP", "120.00", 0)
                                        )
                                )
                        ),
                        new ProductSeed(
                                "OUT-PAD-RIVERRUN",
                                "RiverRun Touring Kayak",
                                "Stable touring kayak with efficient glide, deck storage, and upgrade-ready adventure accessories.",
                                money("799.00"),
                                "EUR",
                                "paddling",
                                productProperties("RiverRun Touring Kayak",
                                        "brand", "RiverRun",
                                        "material", "HDPE",
                                        "rudderReady", true,
                                        "cockpitStyle", "Touring",
                                        "maxLoadKg", 165
                                ),
                                true,
                                variants(
                                        "OUT-PAD-RIVERRUN",
                                        money("799.00"),
                                        5,
                                        axis("length",
                                                choice("12ft", "12", "0.00", 1),
                                                choice("14ft", "14", "160.00", 0)
                                        ),
                                        axis("color",
                                                choice("Blue Wave", "BLU", "0.00", 1),
                                                choice("Sun Yellow", "YLW", "0.00", 0),
                                                choice("Rescue Red", "RED", "20.00", 0)
                                        ),
                                        axis("package",
                                                choice("Solo Kit", "SOL", "0.00", 1),
                                                choice("Adventure Kit", "ADV", "140.00", 0)
                                        )
                                )
                        )
                )
        );
    }

    private StoreSeed fashionStore() {
        return new StoreSeed(
                "Style Lab Fashion",
                "style-lab-fashion",
                List.of(
                        new CategorySeed("fashion", "Fashion", null, map(
                                "department", "fashion",
                                "filterGroups", List.of("size", "fit", "color", "collection")
                        )),
                        new CategorySeed("sneakers", "Sneakers", "fashion", map(
                                "attributes", List.of("fit", "size", "upper", "sole", "color", "lifestyle"),
                                "variantAxes", List.of("color", "size", "fit")
                        )),
                        new CategorySeed("outerwear", "Outerwear", "fashion", map(
                                "attributes", List.of("size", "color", "insulation", "waterproof", "season"),
                                "variantAxes", List.of("color", "size", "insulation")
                        )),
                        new CategorySeed("watches", "Watches", "fashion", map(
                                "attributes", List.of("caseSize", "strap", "dial", "movement", "waterResistance"),
                                "variantAxes", List.of("caseSize", "strap", "dial")
                        ))
                ),
                List.of(
                        new ProductSeed(
                                "FASH-SNK-AEROST",
                                "AeroStreet Runner",
                                "Daily lifestyle sneaker with responsive cushioning, clean lines, and versatile size and fit options.",
                                money("129.00"),
                                "EUR",
                                "sneakers",
                                productProperties("AeroStreet Runner",
                                        "brand", "AeroStreet",
                                        "upper", "Engineered knit",
                                        "midsole", "Dual-density foam",
                                        "outsole", "Rubber traction pods",
                                        "style", "Urban running"
                                ),
                                true,
                                variants(
                                        "FASH-SNK-AEROST",
                                        money("129.00"),
                                        20,
                                        axis("color",
                                                choice("Black", "BLK", "0.00", 2),
                                                choice("White", "WHT", "0.00", 2),
                                                choice("Navy", "NVY", "5.00", 1)
                                        ),
                                        axis("size",
                                                choice("39", "39", "0.00", 2),
                                                choice("40", "40", "0.00", 2),
                                                choice("41", "41", "0.00", 1),
                                                choice("42", "42", "0.00", 1),
                                                choice("43", "43", "0.00", 0)
                                        ),
                                        axis("fit",
                                                choice("Regular", "REG", "0.00", 1),
                                                choice("Wide", "WID", "10.00", 0)
                                        )
                                )
                        ),
                        new ProductSeed(
                                "FASH-OUT-ALPINE",
                                "Alpine Shell Jacket",
                                "Technical waterproof jacket offered in lightweight shell and insulated builds for multi-season wear.",
                                money("219.00"),
                                "EUR",
                                "outerwear",
                                productProperties("Alpine Shell Jacket",
                                        "brand", "North Peak",
                                        "waterproofRating", "20K/20K",
                                        "hood", "Helmet-compatible",
                                        "seamSealed", true,
                                        "packable", true
                                ),
                                true,
                                variants(
                                        "FASH-OUT-ALPINE",
                                        money("219.00"),
                                        11,
                                        axis("color",
                                                choice("Black", "BLK", "0.00", 1),
                                                choice("Moss", "MOS", "10.00", 0),
                                                choice("Signal Orange", "ORG", "10.00", 0)
                                        ),
                                        axis("size",
                                                choice("S", "S", "0.00", 1),
                                                choice("M", "M", "0.00", 1),
                                                choice("L", "L", "0.00", 0),
                                                choice("XL", "XL", "0.00", -1)
                                        ),
                                        axis("insulation",
                                                choice("Shell", "SHL", "0.00", 1),
                                                choice("Insulated", "INS", "70.00", 0)
                                        )
                                )
                        ),
                        new ProductSeed(
                                "FASH-WAT-CHRONO",
                                "Chrono Active Watch",
                                "Hybrid fashion watch with multiple case sizes, premium straps, and dial options for everyday wear.",
                                money("189.00"),
                                "EUR",
                                "watches",
                                productProperties("Chrono Active Watch",
                                        "brand", "Chrono Lab",
                                        "movement", "Japanese quartz",
                                        "waterResistance", "10 ATM",
                                        "crystal", "Sapphire-coated mineral",
                                        "caseMaterial", "Stainless steel"
                                ),
                                true,
                                variants(
                                        "FASH-WAT-CHRONO",
                                        money("189.00"),
                                        12,
                                        axis("caseSize",
                                                choice("40mm", "40", "0.00", 1),
                                                choice("44mm", "44", "20.00", 0)
                                        ),
                                        axis("strap",
                                                choice("Silicone", "SIL", "0.00", 1),
                                                choice("Leather", "LTH", "25.00", 0),
                                                choice("Steel", "STL", "60.00", -1)
                                        ),
                                        axis("dial",
                                                choice("Black", "BLK", "0.00", 1),
                                                choice("Blue", "BLU", "0.00", 0),
                                                choice("Ivory", "IVY", "10.00", 0)
                                        )
                                )
                        )
                )
        );
    }

    private List<VariantSeed> variants(
            String baseSku,
            BigDecimal basePrice,
            int baseStock,
            OptionAxis... axes
    ) {
        List<VariantSeed> variants = new ArrayList<>();
        buildVariantCombinations(
                variants,
                baseSku,
                basePrice,
                baseStock,
                List.of(axes),
                0,
                new LinkedHashMap<>(),
                new ArrayList<>()
        );
        return variants;
    }

    private void buildVariantCombinations(
            List<VariantSeed> variants,
            String baseSku,
            BigDecimal currentPrice,
            int currentStock,
            List<OptionAxis> axes,
            int index,
            Map<String, Object> selectedOptions,
            List<String> skuParts
    ) {
        if (index >= axes.size()) {
            variants.add(new VariantSeed(
                    baseSku + "-" + String.join("-", skuParts),
                    currentPrice,
                    Math.max(currentStock, 0),
                    copyMap(selectedOptions),
                    true
            ));
            return;
        }

        OptionAxis axis = axes.get(index);
        for (OptionChoice choice : axis.choices()) {
            selectedOptions.put(axis.name(), choice.value());
            skuParts.add(choice.skuPart());
            buildVariantCombinations(
                    variants,
                    baseSku,
                    currentPrice.add(choice.priceDelta()),
                    currentStock + choice.stockDelta(),
                    axes,
                    index + 1,
                    selectedOptions,
                    skuParts
            );
            skuParts.remove(skuParts.size() - 1);
            selectedOptions.remove(axis.name());
        }
    }

    private OptionAxis axis(String name, OptionChoice... choices) {
        return new OptionAxis(name, List.of(choices));
    }

    private OptionChoice choice(String value, String skuPart, String priceDelta, int stockDelta) {
        return new OptionChoice(value, skuPart, money(priceDelta), stockDelta);
    }

    private BigDecimal money(String value) {
        return new BigDecimal(value);
    }

    private Map<String, Object> map(Object... keyValues) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            String key = String.valueOf(keyValues[i]);
            Object value = i + 1 < keyValues.length ? keyValues[i + 1] : null;
            map.put(key, value);
        }
        return map;
    }

    private Map<String, Object> productProperties(String productName, Object... keyValues) {
        return map(keyValues);
    }

    private List<String> seedProductImages(
            Store store,
            StoreCategory category,
            ProductSeed productSeed,
            Map<String, Object> properties
    ) {
        List<String> images = new ArrayList<>();
        String storeSlug = sanitizeKeyPart(store.getSlug());
        String productSku = sanitizeKeyPart(productSeed.sku());
        String categoryName = category == null ? humanizeSlug(productSeed.categorySlug()) : category.getName();
        String brand = String.valueOf(properties.getOrDefault("brand", store.getName()));

        for (SeedImageTemplate template : seedImageTemplates()) {
            String key = "linked-store/seeded-products/" + storeSlug + "/" + productSku + "/" + template.fileName() + ".svg";
            String svg = buildSeedImageSvg(productSeed, brand, categoryName, template, properties);
            String url = storageManager.uploadBytes(key, svg.getBytes(StandardCharsets.UTF_8), "image/svg+xml");
            images.add(url);
        }

        return images;
    }

    private String seedStoreBanner(Store store) {
        String key = "linked-store/stores/" + sanitizeKeyPart(store.getSlug()) + "/banner.svg";
        String svg = buildStoreBannerSvg(store);
        return storageManager.uploadBytes(key, svg.getBytes(StandardCharsets.UTF_8), "image/svg+xml");
    }

    private String seedCategoryBanner(Store store, CategorySeed categorySeed, StoreCategory parent) {
        String key = "linked-store/categories/"
                + sanitizeKeyPart(store.getSlug()) + "/"
                + sanitizeKeyPart(categorySeed.slug()) + "/banner.svg";
        String svg = buildCategoryBannerSvg(store, categorySeed, parent);
        return storageManager.uploadBytes(key, svg.getBytes(StandardCharsets.UTF_8), "image/svg+xml");
    }

    private List<String> seedVariantImages(
            Store store,
            ProductSeed productSeed,
            VariantSeed variantSeed,
            StoreCategory category
    ) {
        List<String> images = new ArrayList<>();
        String basePath = "linked-store/variants/"
                + sanitizeKeyPart(store.getSlug()) + "/"
                + sanitizeKeyPart(productSeed.sku()) + "/"
                + sanitizeKeyPart(variantSeed.sku());
        String categoryName = category == null ? humanizeSlug(productSeed.categorySlug()) : category.getName();

        for (SeedImageTemplate template : seedImageTemplates()) {
            String key = basePath + "/" + template.fileName() + ".svg";
            String svg = buildVariantImageSvg(store, productSeed, variantSeed, categoryName, template);
            images.add(storageManager.uploadBytes(key, svg.getBytes(StandardCharsets.UTF_8), "image/svg+xml"));
        }
        return images;
    }

    private List<SeedImageTemplate> seedImageTemplates() {
        return List.of(
                new SeedImageTemplate("hero", "Studio View", "#122033", "#5271FF"),
                new SeedImageTemplate("lifestyle", "Lifestyle View", "#1D3557", "#59A5D8"),
                new SeedImageTemplate("detail", "Detail View", "#2E294E", "#EF8354")
        );
    }

    private String buildSeedImageSvg(
            ProductSeed productSeed,
            String brand,
            String categoryName,
            SeedImageTemplate template,
            Map<String, Object> properties
    ) {
        List<String> highlights = productHighlights(properties);
        StringBuilder highlightMarkup = new StringBuilder();
        for (int index = 0; index < highlights.size(); index++) {
            highlightMarkup.append("""
                    <text x="72" y="%d" font-family="Arial, Helvetica, sans-serif" font-size="26" fill="rgba(255,255,255,0.88)">%s</text>
                    """.formatted(530 + (index * 42), escapeXml(highlights.get(index))));
        }

        return """
                <svg xmlns="http://www.w3.org/2000/svg" width="1200" height="900" viewBox="0 0 1200 900">
                  <defs>
                    <linearGradient id="bg" x1="0%%" y1="0%%" x2="100%%" y2="100%%">
                      <stop offset="0%%" stop-color="%s"/>
                      <stop offset="100%%" stop-color="%s"/>
                    </linearGradient>
                  </defs>
                  <rect width="1200" height="900" fill="url(#bg)"/>
                  <circle cx="1010" cy="140" r="220" fill="rgba(255,255,255,0.10)"/>
                  <circle cx="180" cy="780" r="260" fill="rgba(255,255,255,0.08)"/>
                  <rect x="70" y="82" width="232" height="48" rx="24" fill="rgba(255,255,255,0.16)"/>
                  <text x="106" y="113" font-family="Arial, Helvetica, sans-serif" font-size="24" font-weight="700" fill="#ffffff">%s</text>
                  <text x="72" y="230" font-family="Arial, Helvetica, sans-serif" font-size="34" font-weight="700" fill="rgba(255,255,255,0.88)">%s</text>
                  <text x="72" y="318" font-family="Arial, Helvetica, sans-serif" font-size="74" font-weight="700" fill="#ffffff">%s</text>
                  <text x="72" y="392" font-family="Arial, Helvetica, sans-serif" font-size="28" fill="rgba(255,255,255,0.78)">SKU %s</text>
                  <text x="72" y="456" font-family="Arial, Helvetica, sans-serif" font-size="30" fill="rgba(255,255,255,0.84)">%s</text>
                  %s
                </svg>
                """.formatted(
                template.startColor(),
                template.endColor(),
                escapeXml(template.label()),
                escapeXml(categoryName),
                escapeXml(productSeed.name()),
                escapeXml(productSeed.sku()),
                escapeXml(brand),
                highlightMarkup
        );
    }

    private String buildStoreBannerSvg(Store store) {
        return """
                <svg xmlns="http://www.w3.org/2000/svg" width="1600" height="560" viewBox="0 0 1600 560">
                  <defs>
                    <linearGradient id="storeBg" x1="0%%" y1="0%%" x2="100%%" y2="100%%">
                      <stop offset="0%%" stop-color="#122033"/>
                      <stop offset="100%%" stop-color="#5271FF"/>
                    </linearGradient>
                  </defs>
                  <rect width="1600" height="560" fill="url(#storeBg)"/>
                  <circle cx="1260" cy="130" r="210" fill="rgba(255,255,255,0.10)"/>
                  <circle cx="320" cy="500" r="260" fill="rgba(255,255,255,0.08)"/>
                  <text x="96" y="118" font-family="Arial, Helvetica, sans-serif" font-size="32" font-weight="700" fill="rgba(255,255,255,0.78)">Linked Store</text>
                  <text x="96" y="240" font-family="Arial, Helvetica, sans-serif" font-size="82" font-weight="700" fill="#ffffff">%s</text>
                  <text x="96" y="318" font-family="Arial, Helvetica, sans-serif" font-size="34" fill="rgba(255,255,255,0.84)">Seeded storefront banner stored in R2</text>
                  <text x="96" y="388" font-family="Arial, Helvetica, sans-serif" font-size="28" fill="rgba(255,255,255,0.72)">Slug: %s</text>
                </svg>
                """.formatted(escapeXml(store.getName()), escapeXml(store.getSlug()));
    }

    private String buildCategoryBannerSvg(Store store, CategorySeed categorySeed, StoreCategory parent) {
        String parentName = parent == null ? store.getName() : parent.getName();
        return """
                <svg xmlns="http://www.w3.org/2000/svg" width="1600" height="440" viewBox="0 0 1600 440">
                  <defs>
                    <linearGradient id="categoryBg" x1="0%%" y1="0%%" x2="100%%" y2="100%%">
                      <stop offset="0%%" stop-color="#1D3557"/>
                      <stop offset="100%%" stop-color="#59A5D8"/>
                    </linearGradient>
                  </defs>
                  <rect width="1600" height="440" fill="url(#categoryBg)"/>
                  <rect x="80" y="72" width="290" height="54" rx="27" fill="rgba(255,255,255,0.16)"/>
                  <text x="116" y="107" font-family="Arial, Helvetica, sans-serif" font-size="28" font-weight="700" fill="#ffffff">%s</text>
                  <text x="80" y="220" font-family="Arial, Helvetica, sans-serif" font-size="72" font-weight="700" fill="#ffffff">%s</text>
                  <text x="80" y="292" font-family="Arial, Helvetica, sans-serif" font-size="30" fill="rgba(255,255,255,0.80)">Parent: %s</text>
                </svg>
                """.formatted(
                escapeXml(store.getName()),
                escapeXml(categorySeed.name()),
                escapeXml(parentName)
        );
    }

    private String buildVariantImageSvg(
            Store store,
            ProductSeed productSeed,
            VariantSeed variantSeed,
            String categoryName,
            SeedImageTemplate template
    ) {
        List<String> highlights = productHighlights(variantSeed.options());
        StringBuilder highlightMarkup = new StringBuilder();
        for (int index = 0; index < highlights.size(); index++) {
            highlightMarkup.append("""
                    <text x="72" y="%d" font-family="Arial, Helvetica, sans-serif" font-size="26" fill="rgba(255,255,255,0.88)">%s</text>
                    """.formatted(520 + (index * 40), escapeXml(highlights.get(index))));
        }

        return """
                <svg xmlns="http://www.w3.org/2000/svg" width="1200" height="900" viewBox="0 0 1200 900">
                  <defs>
                    <linearGradient id="variantBg" x1="0%%" y1="0%%" x2="100%%" y2="100%%">
                      <stop offset="0%%" stop-color="%s"/>
                      <stop offset="100%%" stop-color="%s"/>
                    </linearGradient>
                  </defs>
                  <rect width="1200" height="900" fill="url(#variantBg)"/>
                  <circle cx="980" cy="140" r="190" fill="rgba(255,255,255,0.12)"/>
                  <text x="72" y="112" font-family="Arial, Helvetica, sans-serif" font-size="28" font-weight="700" fill="rgba(255,255,255,0.76)">%s</text>
                  <text x="72" y="220" font-family="Arial, Helvetica, sans-serif" font-size="34" font-weight="700" fill="rgba(255,255,255,0.88)">%s</text>
                  <text x="72" y="302" font-family="Arial, Helvetica, sans-serif" font-size="68" font-weight="700" fill="#ffffff">%s</text>
                  <text x="72" y="376" font-family="Arial, Helvetica, sans-serif" font-size="28" fill="rgba(255,255,255,0.78)">%s</text>
                  <text x="72" y="438" font-family="Arial, Helvetica, sans-serif" font-size="30" fill="rgba(255,255,255,0.82)">%s</text>
                  %s
                </svg>
                """.formatted(
                template.startColor(),
                template.endColor(),
                escapeXml(store.getName()),
                escapeXml(categoryName),
                escapeXml(productSeed.name()),
                escapeXml(variantSeed.sku()),
                escapeXml(template.label()),
                highlightMarkup
        );
    }

    private List<String> productHighlights(Map<String, Object> properties) {
        List<String> highlights = new ArrayList<>();
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            if ("images".equals(entry.getKey())) {
                continue;
            }
            String value = formatPropertyValue(entry.getValue());
            if (value.isBlank()) {
                continue;
            }
            highlights.add(humanizeSlug(entry.getKey()) + ": " + value);
            if (highlights.size() == 3) {
                break;
            }
        }
        if (highlights.isEmpty()) {
            highlights.add("Seeded gallery image");
        }
        return highlights;
    }

    private String formatPropertyValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof List<?> list) {
            return list.stream().limit(3).map(String::valueOf).reduce((left, right) -> left + ", " + right).orElse("");
        }
        return String.valueOf(value);
    }

    private String sanitizeKeyPart(String value) {
        String normalized = String.valueOf(value == null ? "" : value).trim().toLowerCase(Locale.ROOT);
        normalized = normalized.replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return normalized.isBlank() ? "item" : normalized;
    }

    private String humanizeSlug(String value) {
        String normalized = String.valueOf(value == null ? "" : value).trim().replace('-', ' ').replace('_', ' ');
        if (normalized.isBlank()) {
            return "";
        }
        String[] parts = normalized.split("\\s+");
        StringBuilder humanized = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (!humanized.isEmpty()) {
                humanized.append(' ');
            }
            humanized.append(part.substring(0, 1).toUpperCase(Locale.ROOT));
            if (part.length() > 1) {
                humanized.append(part.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return humanized.toString();
    }

    private String escapeXml(String value) {
        return String.valueOf(value == null ? "" : value)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private Map<String, Object> copyMap(Map<String, Object> source) {
        return source == null ? new LinkedHashMap<>() : new LinkedHashMap<>(source);
    }

    private record StoreSeed(
            String name,
            String slug,
            List<CategorySeed> categories,
            List<ProductSeed> products
    ) {
    }

    private record CategorySeed(
            String slug,
            String name,
            String parentSlug,
            Map<String, Object> attributeSchema
    ) {
    }

    private record ProductSeed(
            String sku,
            String name,
            String description,
            BigDecimal basePrice,
            String currency,
            String categorySlug,
            Map<String, Object> properties,
            boolean active,
            List<VariantSeed> variants
    ) {
    }

    private record VariantSeed(
            String sku,
            BigDecimal price,
            int stock,
            Map<String, Object> options,
            boolean active
    ) {
    }

    private record OptionAxis(String name, List<OptionChoice> choices) {
    }

    private record OptionChoice(
            String value,
            String skuPart,
            BigDecimal priceDelta,
            int stockDelta
    ) {
    }

    private record SeedImageTemplate(
            String fileName,
            String label,
            String startColor,
            String endColor
    ) {
    }
}

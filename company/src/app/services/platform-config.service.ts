import { computed, Injectable, signal } from '@angular/core';
import {
  FooterLinkGroup,
  LanguageCode,
  NavItem,
  ProductSolution,
  SiteConfig,
  SiteLocaleContent,
  SupportedLanguage
} from '../models/landing.models';

type VisibilityInput = Record<string, boolean> | boolean[];

interface ItemVisibilityInput {
  navLinks?: VisibilityInput;
  platformCards?: VisibilityInput;
  testimonial?: boolean;
  footerGroups?: VisibilityInput;
}

interface LandingPageConfigFile {
  defaultLanguage?: LanguageCode;
  supportedLanguages?: SiteConfig['supportedLanguages'];
  sectionVisibility?: Partial<SiteConfig['sectionVisibility']>;
  'section-visiility'?: Partial<SiteConfig['sectionVisibility']>;
  'section-visibility'?: Partial<SiteConfig['sectionVisibility']>;
  itemVisibility?: ItemVisibilityInput;
  'item-visiility'?: ItemVisibilityInput;
  'item-visibility'?: ItemVisibilityInput;
  locales?: Partial<Record<LanguageCode, Partial<SiteLocaleContent>>>;
}

const LOCALE_ORDER: LanguageCode[] = ['en', 'de', 'pt', 'fr'];

const DEFAULT_LANGUAGE_LABELS: Record<LanguageCode, string> = {
  en: 'English',
  de: 'Deutsch',
  pt: 'Portugues',
  fr: 'Francais'
};

const DEFAULT_SECTION_VISIBILITY: SiteConfig['sectionVisibility'] = {
  nav: true,
  hero: true,
  platformSection: true,
  testimonial: true,
  finalCta: true,
  footer: true
};

function createEmptyLocaleContent(): SiteLocaleContent {
  return {
    nav: {
      brand: { mark: '', name: '', ariaLabel: '' },
      links: []
    },
    hero: {
      showSection: false,
      badge: '',
      title: '',
      accent: '',
      description: ''
    },
    platformSection: {
      showSection: false,
      eyebrow: '',
      title: '',
      subtitle: '',
      ctaLabel: '',
      ctaHref: '',
      platforms: []
    },
    testimonial: {
      showSection: false,
      eyebrow: '',
      title: '',
      quote: '',
      author: '',
      authorRole: ''
    },
    finalCta: {
      showSection: false,
      title: '',
      description: '',
      primaryCta: { label: '', href: '' },
      secondaryCta: { label: '', href: '' }
    },
    footer: {
      showSection: false,
      brand: { mark: '', name: '', ariaLabel: '' },
      description: '',
      groups: [],
      contactTitle: '',
      email: '',
      phone: '',
      address: '',
      bottomLeft: '',
      bottomRight: ''
    }
  };
}

function createEmptySiteConfig(): SiteConfig {
  const emptyLocales = LOCALE_ORDER.reduce<Record<LanguageCode, SiteLocaleContent>>((acc, code) => {
    acc[code] = createEmptyLocaleContent();
    return acc;
  }, {} as Record<LanguageCode, SiteLocaleContent>);

  return {
    defaultLanguage: 'en',
    supportedLanguages: LOCALE_ORDER.map((code) => ({ code, label: DEFAULT_LANGUAGE_LABELS[code] })),
    sectionVisibility: { ...DEFAULT_SECTION_VISIBILITY, nav: false, hero: false, platformSection: false, testimonial: false, finalCta: false, footer: false },
    itemVisibility: {
      navLinks: {},
      platformCards: {},
      testimonial: true,
      footerGroups: {}
    },
    locales: emptyLocales
  };
}

@Injectable({
  providedIn: 'root'
})
export class PlatformConfigService {
  readonly siteConfig = signal<SiteConfig>(createEmptySiteConfig());
  readonly currentLanguage = signal<LanguageCode>('en');
  readonly supportedLanguages = computed(() => this.siteConfig().supportedLanguages);
  readonly locale = computed(
    () => this.siteConfig().locales[this.currentLanguage()] ?? this.siteConfig().locales[this.siteConfig().defaultLanguage]
  );

  async load(): Promise<void> {
    try {
      const response = await fetch('/data.json', { cache: 'no-store' });
      if (!response.ok) {
        throw new Error(`Failed to load /data.json: ${response.status}`);
      }

      const config = (await response.json()) as LandingPageConfigFile;
      if (!config.locales) {
        throw new Error('Missing locales in /data.json');
      }

      const normalizedConfig = this.normalizeConfig(config);
      this.siteConfig.set(normalizedConfig);
      this.currentLanguage.set(normalizedConfig.defaultLanguage);
    } catch (error) {
      console.error('Failed to load site content from data.json.', error);
    }
  }

  setLanguage(language: LanguageCode): void {
    if (this.siteConfig().locales[language]) {
      this.currentLanguage.set(language);
    }
  }

  private applySectionVisibility(
    locale: SiteLocaleContent,
    sectionVisibility: SiteConfig['sectionVisibility']
  ): SiteLocaleContent {
    return {
      ...locale,
      hero: { ...locale.hero, showSection: sectionVisibility.hero && locale.hero.showSection },
      platformSection: {
        ...locale.platformSection,
        showSection: sectionVisibility.platformSection && locale.platformSection.showSection
      },
      testimonial: {
        ...locale.testimonial,
        showSection: sectionVisibility.testimonial && locale.testimonial.showSection
      },
      finalCta: { ...locale.finalCta, showSection: sectionVisibility.finalCta && locale.finalCta.showSection },
      footer: { ...locale.footer, showSection: sectionVisibility.footer && locale.footer.showSection }
    };
  }

  private applyItemVisibility(
    locale: SiteLocaleContent,
    itemVisibility: SiteConfig['itemVisibility']
  ): SiteLocaleContent {
    return {
      ...locale,
      nav: {
        ...locale.nav,
        links: locale.nav.links.filter((item) => item.visible !== false && itemVisibility.navLinks[item.id] !== false)
      },
      platformSection: {
        ...locale.platformSection,
        platforms: locale.platformSection.platforms.map((product) => ({
          ...product,
          visible: product.visible !== false && itemVisibility.platformCards[product.id] !== false
        }))
      },
      testimonial: {
        ...locale.testimonial,
        showSection: locale.testimonial.showSection && itemVisibility.testimonial !== false
      },
      footer: {
        ...locale.footer,
        groups: locale.footer.groups.filter((group) => itemVisibility.footerGroups[group.id] !== false)
      }
    };
  }

  private normalizeConfig(config: LandingPageConfigFile): SiteConfig {
    const sectionVisibilityInput =
      config.sectionVisibility ?? config['section-visiility'] ?? config['section-visibility'] ?? {};
    const normalizedLocales = this.normalizeLocales(config.locales ?? {});
    const normalizedItemVisibility = this.normalizeItemVisibility(
      config.itemVisibility ?? config['item-visiility'] ?? config['item-visibility'] ?? {},
      normalizedLocales
    );
    const sectionVisibility = {
      ...DEFAULT_SECTION_VISIBILITY,
      ...sectionVisibilityInput
    };

    return {
      defaultLanguage: this.resolveDefaultLanguage(config.defaultLanguage, config.locales),
      supportedLanguages: this.resolveSupportedLanguages(config.supportedLanguages, config.locales),
      sectionVisibility,
      itemVisibility: normalizedItemVisibility,
      locales: {
        en: this.applyItemVisibility(this.applySectionVisibility(normalizedLocales.en, sectionVisibility), normalizedItemVisibility),
        de: this.applyItemVisibility(this.applySectionVisibility(normalizedLocales.de, sectionVisibility), normalizedItemVisibility),
        pt: this.applyItemVisibility(this.applySectionVisibility(normalizedLocales.pt, sectionVisibility), normalizedItemVisibility),
        fr: this.applyItemVisibility(this.applySectionVisibility(normalizedLocales.fr, sectionVisibility), normalizedItemVisibility)
      }
    };
  }

  private normalizeLocales(
    locales: Partial<Record<LanguageCode, Partial<SiteLocaleContent>>>
  ): Record<LanguageCode, SiteLocaleContent> {
    return {
      en: this.normalizeLocale(locales.en),
      de: this.normalizeLocale(locales.de),
      pt: this.normalizeLocale(locales.pt),
      fr: this.normalizeLocale(locales.fr)
    };
  }

  private normalizeLocale(locale: Partial<SiteLocaleContent> | undefined): SiteLocaleContent {
    const empty = createEmptyLocaleContent();

    return {
      nav: {
        brand: {
          ...empty.nav.brand,
          ...locale?.nav?.brand
        },
        links: this.withStableNavIds(locale?.nav?.links ?? [])
      },
      hero: {
        ...empty.hero,
        ...locale?.hero
      },
      platformSection: {
        ...empty.platformSection,
        ...locale?.platformSection,
        platforms: this.withStableProductIds(locale?.platformSection?.platforms ?? [])
      },
      testimonial: {
        ...empty.testimonial,
        ...locale?.testimonial
      },
      finalCta: {
        ...empty.finalCta,
        ...locale?.finalCta,
        primaryCta: {
          ...empty.finalCta.primaryCta,
          ...locale?.finalCta?.primaryCta
        },
        secondaryCta: {
          ...empty.finalCta.secondaryCta,
          ...locale?.finalCta?.secondaryCta
        }
      },
      footer: {
        ...empty.footer,
        ...locale?.footer,
        brand: {
          ...empty.footer.brand,
          ...locale?.footer?.brand
        },
        groups: this.withStableFooterGroupIds(locale?.footer?.groups ?? [])
      }
    };
  }

  private normalizeItemVisibility(
    itemVisibility: ItemVisibilityInput,
    locales: Record<LanguageCode, SiteLocaleContent>
  ): SiteConfig['itemVisibility'] {
    const navIds = this.collectIds(locales, (locale) => locale.nav.links.map((item) => item.id));
    const platformIds = this.collectIds(locales, (locale) => locale.platformSection.platforms.map((item) => item.id));
    const footerGroupIds = this.collectIds(locales, (locale) => locale.footer.groups.map((item) => item.id));

    return {
      navLinks: this.toVisibilityMap(itemVisibility.navLinks, navIds),
      platformCards: this.toVisibilityMap(itemVisibility.platformCards, platformIds),
      testimonial: itemVisibility.testimonial ?? true,
      footerGroups: this.toVisibilityMap(itemVisibility.footerGroups, footerGroupIds)
    };
  }

  private toVisibilityMap(value: VisibilityInput | undefined, orderedKeys: string[]): Record<string, boolean> {
    if (Array.isArray(value)) {
      return orderedKeys.reduce<Record<string, boolean>>((acc, key, index) => {
        acc[key] = value[index] ?? true;
        return acc;
      }, {});
    }

    return { ...(value ?? {}) };
  }

  private collectIds(
    locales: Record<LanguageCode, SiteLocaleContent>,
    extractor: (locale: SiteLocaleContent) => string[]
  ): string[] {
    return [...new Set(LOCALE_ORDER.flatMap((code) => extractor(locales[code]).filter(Boolean)))];
  }

  private withStableNavIds(items: NavItem[]): NavItem[] {
    return items.map((item, index) => ({
      ...item,
      id: item.id ?? this.createStableId(item.label, `nav-${index}`)
    }));
  }

  private withStableProductIds(items: ProductSolution[]): ProductSolution[] {
    return items.map((item, index) => ({
      ...item,
      id: item.id ?? this.createStableId(item.label || item.title, `platform-${index}`)
    }));
  }

  private withStableFooterGroupIds(items: FooterLinkGroup[]): FooterLinkGroup[] {
    return items.map((item, index) => ({
      ...item,
      id: item.id ?? this.createStableId(item.title, `footer-${index}`)
    }));
  }

  private createStableId(value: string | undefined, fallback: string): string {
    const normalized = (value ?? '')
      .toLowerCase()
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/^-+|-+$/g, '');

    return normalized || fallback;
  }

  private resolveDefaultLanguage(
    configuredLanguage: LanguageCode | undefined,
    locales: Partial<Record<LanguageCode, Partial<SiteLocaleContent>>> | undefined
  ): LanguageCode {
    if (configuredLanguage && locales?.[configuredLanguage]) {
      return configuredLanguage;
    }

    return LOCALE_ORDER.find((code) => locales?.[code]) ?? 'en';
  }

  private resolveSupportedLanguages(
    configuredLanguages: SupportedLanguage[] | undefined,
    locales: Partial<Record<LanguageCode, Partial<SiteLocaleContent>>> | undefined
  ): SupportedLanguage[] {
    if (configuredLanguages?.length) {
      return configuredLanguages;
    }

    const available = LOCALE_ORDER.filter((code) => locales?.[code]);

    return (available.length ? available : LOCALE_ORDER).map((code) => ({
      code,
      label: DEFAULT_LANGUAGE_LABELS[code]
    }));
  }
}

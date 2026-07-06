export type LanguageCode = 'en' | 'de' | 'pt' | 'fr';

export interface NavItem {
  id: string;
  label: string;
  href: string;
  visible?: boolean;
}

export interface SupportedLanguage {
  code: LanguageCode;
  label: string;
}

export interface StatItem {
  value: string;
  accent?: string;
  label: string;
}

export interface ValueProp {
  icon: 'analytics' | 'roi' | 'spark';
  title: string;
  description: string;
}

export interface BrandConfig {
  mark: string;
  name: string;
  ariaLabel: string;
}

export interface NavConfig {
  brand: BrandConfig;
  links: NavItem[];
}

export interface HeroSectionConfig {
  showSection: boolean;
  badge: string;
  title: string;
  accent: string;
  description: string;
}

export interface ProductSolution {
  id: string;
  label: string;
  title: string;
  tagline: string;
  description: string;
  features: string[];
  image: string;
  alt: string;
  ctaLabel?: string;
  link?: string;
  visible?: boolean;
}

export interface PlatformSectionConfig {
  showSection: boolean;
  eyebrow: string;
  title: string;
  subtitle: string;
  ctaLabel: string;
  ctaHref: string;
  platforms: ProductSolution[];
}

export interface TestimonialSectionConfig {
  showSection: boolean;
  eyebrow: string;
  title: string;
  quote: string;
  author: string;
  authorRole: string;
}

export interface CtaButtonConfig {
  label: string;
  href: string;
}

export interface FinalCtaSectionConfig {
  showSection: boolean;
  title: string;
  description: string;
  primaryCta: CtaButtonConfig;
  secondaryCta: CtaButtonConfig;
}

export interface AboutProjectConfig {
  id: string;
  label: string;
  title: string;
  description: string;
  points: string[];
}

export interface AboutBenefitConfig {
  title: string;
  description: string;
}

export interface AboutSectionConfig {
  badge: string;
  title: string;
  accent: string;
  lead: string;
  intro: string[];
  projectsEyebrow: string;
  projectsTitle: string;
  projectsSubtitle: string;
  projects: AboutProjectConfig[];
  visionEyebrow: string;
  visionTitle: string;
  visionDescription: string;
  visionPrinciples: string[];
  benefitsEyebrow: string;
  benefitsTitle: string;
  benefits: AboutBenefitConfig[];
  closingTitle: string;
  closingDescription: string;
  closingCta: CtaButtonConfig;
}

export interface ContactFieldConfig {
  label: string;
  placeholder: string;
}

export interface ContactSectionConfig {
  badge: string;
  title: string;
  description: string;
  panelTitle: string;
  panelDescription: string;
  highlights: string[];
  phoneLabel: string;
  addressLabel: string;
  fullName: ContactFieldConfig;
  email: ContactFieldConfig;
  company: ContactFieldConfig;
  solution: ContactFieldConfig;
  message: ContactFieldConfig;
  generalInquiryLabel: string;
  sendingLabel: string;
  submitLabel: string;
  backToHomeLabel: string;
  successMessage: string;
  errorMessage: string;
}

export interface FooterLinkGroup {
  id: string;
  title: string;
  links: { label: string; href: string }[];
}

export interface FooterConfig {
  showSection: boolean;
  brand: BrandConfig;
  description: string;
  groups: FooterLinkGroup[];
  contactTitle: string;
  email: string;
  phone: string;
  address: string;
  bottomLeft: string;
  bottomRight: string;
}

export interface SectionVisibilityConfig {
  nav: boolean;
  hero: boolean;
  platformSection: boolean;
  testimonial: boolean;
  finalCta: boolean;
  footer: boolean;
}

export interface ItemVisibilityConfig {
  navLinks: Record<string, boolean>;
  platformCards: Record<string, boolean>;
  testimonial: boolean;
  footerGroups: Record<string, boolean>;
}

export interface SiteLocaleContent {
  nav: NavConfig;
  hero: HeroSectionConfig;
  about: AboutSectionConfig;
  contact: ContactSectionConfig;
  platformSection: PlatformSectionConfig;
  testimonial: TestimonialSectionConfig;
  finalCta: FinalCtaSectionConfig;
  footer: FooterConfig;
}

export interface SiteConfig {
  defaultLanguage: LanguageCode;
  supportedLanguages: SupportedLanguage[];
  sectionVisibility: SectionVisibilityConfig;
  itemVisibility: ItemVisibilityConfig;
  locales: Record<LanguageCode, SiteLocaleContent>;
}

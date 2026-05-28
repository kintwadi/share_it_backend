import { Injectable, signal } from '@angular/core';

type SupportedLanguage = 'en' | 'pt' | 'de';
type SupportedCurrency = 'EUR' | 'USD' | 'GBP';

const TRANSLATIONS: Record<SupportedLanguage, Record<string, string>> = {
  en: {
    'home.hero_title_1': 'Share more. Own less.',
    'home.hero_desc': 'Borrow, lend, and give within your neighborhood — trusted, fast, and local.'
  },
  pt: {
    'home.hero_title_1': 'Partilhe mais. Possua menos.',
    'home.hero_desc': 'Empreste, peça emprestado e ofereça na sua vizinhança — com confiança, rapidez e proximidade.'
  },
  de: {
    'home.hero_title_1': 'Mehr teilen. Weniger besitzen.',
    'home.hero_desc': 'Leihen, verleihen und verschenken in deiner Nachbarschaft — vertrauenswürdig, schnell und lokal.'
  }
};

@Injectable({
  providedIn: 'root'
})
export class I18nService {
  private languageKey = 'i18n_language';
  private currencyKey = 'i18n_currency';

  language = signal<SupportedLanguage>(this.readLanguage());
  currency = signal<SupportedCurrency>(this.readCurrency());

  supportedCurrencies: SupportedCurrency[] = ['EUR', 'USD', 'GBP'];

  t(key: string): string {
    const k = String(key || '').trim();
    if (!k) return '';
    const lang = this.language();
    return TRANSLATIONS[lang]?.[k] ?? k;
  }

  formatPrice(amount: number, currency?: SupportedCurrency): string {
    const value = Number(amount || 0);
    const curr = (currency || this.currency()) as any;
    const lang = this.language();
    const locale = lang === 'de' ? 'de-DE' : lang === 'pt' ? 'pt-PT' : 'en-GB';
    try {
      return new Intl.NumberFormat(locale, { style: 'currency', currency: curr }).format(value);
    } catch {
      return `${curr} ${value.toFixed(2)}`;
    }
  }

  formatDateTime(d: Date | string | number): string {
    const dt = d instanceof Date ? d : new Date(d);
    const lang = this.language();
    const locale = lang === 'de' ? 'de-DE' : lang === 'pt' ? 'pt-PT' : 'en-GB';
    if (isNaN(dt.getTime())) return '';
    try {
      return dt.toLocaleString(locale);
    } catch {
      return dt.toISOString();
    }
  }

  setLanguage(lang: SupportedLanguage) {
    this.language.set(lang);
    try {
      localStorage.setItem(this.languageKey, lang);
    } catch { }
  }

  setCurrency(curr: SupportedCurrency) {
    this.currency.set(curr);
    try {
      localStorage.setItem(this.currencyKey, curr);
    } catch { }
  }

  languageLabel(lang?: any): string {
    const l = String(lang || this.language()).toLowerCase();
    if (l === 'en') return 'English';
    if (l === 'pt') return 'Português';
    if (l === 'de') return 'Deutsch';
    return String(lang || this.language());
  }

  private readLanguage(): SupportedLanguage {
    try {
      const v = String(localStorage.getItem(this.languageKey) || '').trim().toLowerCase();
      if (v === 'en' || v === 'pt' || v === 'de') return v;
    } catch { }
    return 'en';
  }

  private readCurrency(): SupportedCurrency {
    try {
      const v = String(localStorage.getItem(this.currencyKey) || '').trim().toUpperCase();
      if (v === 'EUR' || v === 'USD' || v === 'GBP') return v;
    } catch { }
    return 'EUR';
  }
}

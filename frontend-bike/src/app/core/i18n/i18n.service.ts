import { Injectable, computed, signal } from '@angular/core';
import { DE_TRANSLATIONS } from './translations/de';
import { EN_TRANSLATIONS } from './translations/en';
import { LanguageCode, TranslationTree, TranslationValue } from './i18n.types';

@Injectable({ providedIn: 'root' })
export class I18nService {
  private readonly storageKey = 'frontend-bike.language';
  private readonly dictionary = {
    en: EN_TRANSLATIONS,
    de: DE_TRANSLATIONS
  } as const;

  readonly availableLanguages: { code: LanguageCode; labelKey: string }[] = [
    { code: 'en', labelKey: 'common.english' },
    { code: 'de', labelKey: 'common.german' }
  ];

  readonly language = signal<LanguageCode>(this.resolveInitialLanguage());
  readonly translations = computed(() => this.dictionary[this.language()]);

  setLanguage(language: LanguageCode): void {
    this.language.set(language);
    if (typeof window !== 'undefined') {
      window.localStorage.setItem(this.storageKey, language);
    }
  }

  t(key: string, params?: Record<string, string | number | null | undefined>): string {
    const value = this.lookup(key, this.translations());
    if (typeof value !== 'string') {
      return key;
    }

    return this.interpolate(value, params);
  }

  private resolveInitialLanguage(): LanguageCode {
    if (typeof window === 'undefined') {
      return 'en';
    }

    const stored = window.localStorage.getItem(this.storageKey);
    if (stored === 'en' || stored === 'de') {
      return stored;
    }

    return window.navigator.language.toLowerCase().startsWith('de') ? 'de' : 'en';
  }

  private lookup(key: string, tree: TranslationTree): TranslationValue | undefined {
    return key.split('.').reduce<TranslationValue | undefined>((current, segment) => {
      if (!current || typeof current === 'string') {
        return undefined;
      }

      return current[segment];
    }, tree);
  }

  private interpolate(template: string, params?: Record<string, string | number | null | undefined>): string {
    if (!params) {
      return template;
    }

    return template.replace(/\{(\w+)\}/g, (_, token: string) => `${params[token] ?? ''}`);
  }
}

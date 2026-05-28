import { ApplicationRef, Injectable, inject, signal } from '@angular/core';

type SupportedLanguage = 'en' | 'pt' | 'de';
type SupportedCurrency = 'EUR' | 'USD' | 'GBP' | 'BRL';

const TRANSLATIONS: Record<SupportedLanguage, Record<string, string>> = {
  en: {
    'common.back': 'Back',
    'common.cancel': 'Cancel',
    'common.confirm': 'Confirm',
    'common.copied': 'Copied',
    'common.error': 'Error',
    'common.loading': 'Loading…',

    'home.hero_title_1': 'Share more. Own less.',
    'home.hero_desc': 'Borrow, lend, and give within your neighborhood — trusted, fast, and local.',
    'home.filter_all': 'All',
    'home.filter_goods': 'Items',
    'home.filter_skills': 'Skills',
    'home.category_all': 'All categories',

    'nav.discover': 'Discover',
    'nav.enterprise': 'Enterprise',
    'nav.dashboard': 'Dashboard',
    'nav.admin': 'Admin',
    'nav.messages': 'Messages',
    'nav.subscribe': 'Subscribe',

    'connect.welcome_back': 'Welcome back',
    'connect.create_account_title': 'Create your account',
    'connect.login_help': 'Sign in to continue.',
    'connect.register_help': 'Create an account to get started.',
    'connect.sign_in': 'Sign in',
    'connect.register': 'Register',
    'connect.full_name': 'Full name',
    'connect.email': 'Email',
    'connect.password': 'Password',
    'connect.reset_password': 'Reset password',
    'connect.create_account_cta': 'Create account',
    'connect.terms.prefix': 'By continuing, you agree to our',
    'connect.terms.terms': 'Terms',
    'connect.terms.and': 'and',
    'connect.terms.privacy': 'Privacy Policy',
    'connect.demo.trust98': 'Trust 98',
    'connect.demo.unverified': 'Unverified',
    'connect.footer_info': 'You can change your preferences later.',
    'connect.privacy.title': 'Privacy',
    'connect.privacy.agree': 'We only use your data to run the service.',
    'connect.create_free_account': 'Create free account',
    'connect.trust.title': 'Trust score',
    'connect.trust.help': 'Verified profiles build safer sharing.',
    'connect.hyperlocal.title': 'Hyper-local',
    'connect.hyperlocal.help': 'Find what you need nearby.',

    'connect.privacy.bullet1': 'No spam. No selling your data.',
    'connect.privacy.bullet2': 'You control what you share and with whom.',
    'connect.privacy.bullet3': 'Verified neighbors, safer sharing.',

    'lenderSub.loading': 'Loading…',

    'subscription.back_to_discovery': 'Back to discovery',
    'subscription.title': 'Choose your plan',
    'subscription.select': 'Select',
    'subscription.recommended': 'Recommended',

    'subscription.starter.subtitle': 'Starter',
    'subscription.starter.title': 'Free',
    'subscription.starter.price': '€0 / month',
    'subscription.starter.feature1': 'Browse and request items nearby',
    'subscription.starter.feature2': 'Message owners and coordinate pickup',
    'subscription.starter.feature3': 'Basic trust and safety checks',
    'subscription.starter.feature4': 'Standard support',

    'subscription.plus.subtitle': 'Plus',
    'subscription.plus.title': 'Verified',
    'subscription.plus.price': '€4.99 / month',
    'subscription.plus.trial': '14-day free trial',
    'subscription.plus.feature1': 'Verified borrower badge',
    'subscription.plus.feature2': 'Priority approval for some listings',
    'subscription.plus.feature3': 'Reduced service fees where applicable',
    'subscription.plus.feature4': 'Access to protection options on eligible items',
    'subscription.plus.feature5': 'Priority support',

    'subscription.pro.subtitle': 'Pro',
    'subscription.pro.title': 'Premium Lender',
    'subscription.pro.price': 'Coming soon',
    'subscription.pro.trial': 'Limited rollout',
    'subscription.pro.feature1': 'Higher visibility for your listings',
    'subscription.pro.feature2': 'Advanced analytics',
    'subscription.pro.feature3': 'Faster payouts',
    'subscription.pro.feature4': 'Custom policies',
    'subscription.pro.feature5': 'Dedicated support',
    'subscription.pro.feature6': 'Team/organization features'
  },
  pt: {
    'common.back': 'Voltar',
    'common.cancel': 'Cancelar',
    'common.confirm': 'Confirmar',
    'common.copied': 'Copiado',
    'common.error': 'Erro',
    'common.loading': 'A carregar…',

    'home.hero_title_1': 'Partilhe mais. Possua menos.',
    'home.hero_desc': 'Empreste, peça emprestado e ofereça na sua vizinhança — com confiança, rapidez e proximidade.',
    'home.filter_all': 'Tudo',
    'home.filter_goods': 'Itens',
    'home.filter_skills': 'Competências',
    'home.category_all': 'Todas as categorias',

    'nav.discover': 'Descobrir',
    'nav.enterprise': 'Empresas',
    'nav.dashboard': 'Painel',
    'nav.admin': 'Admin',
    'nav.messages': 'Mensagens',
    'nav.subscribe': 'Subscrever',

    'connect.welcome_back': 'Bem-vindo(a) de volta',
    'connect.create_account_title': 'Criar conta',
    'connect.login_help': 'Inicie sessão para continuar.',
    'connect.register_help': 'Crie uma conta para começar.',
    'connect.sign_in': 'Iniciar sessão',
    'connect.register': 'Registar',
    'connect.full_name': 'Nome completo',
    'connect.email': 'Email',
    'connect.password': 'Palavra-passe',
    'connect.reset_password': 'Repor palavra-passe',
    'connect.create_account_cta': 'Criar conta',
    'connect.terms.prefix': 'Ao continuar, você concorda com os nossos',
    'connect.terms.terms': 'Termos',
    'connect.terms.and': 'e',
    'connect.terms.privacy': 'Política de Privacidade',
    'connect.demo.trust98': 'Confiança 98',
    'connect.demo.unverified': 'Não verificado',
    'connect.footer_info': 'Pode alterar as preferências mais tarde.',
    'connect.privacy.title': 'Privacidade',
    'connect.privacy.agree': 'Usamos os seus dados apenas para operar o serviço.',
    'connect.create_free_account': 'Criar conta gratuita',
    'connect.trust.title': 'Pontuação de confiança',
    'connect.trust.help': 'Perfis verificados tornam a partilha mais segura.',
    'connect.hyperlocal.title': 'Hiperlocal',
    'connect.hyperlocal.help': 'Encontre o que precisa perto de si.',
    'connect.privacy.bullet1': 'Sem spam. Sem vender os seus dados.',
    'connect.privacy.bullet2': 'Você controla o que partilha e com quem.',
    'connect.privacy.bullet3': 'Vizinhos verificados, partilhas mais seguras.',


    'lenderSub.loading': 'A carregar…',

    'subscription.back_to_discovery': 'Voltar à descoberta',
    'subscription.title': 'Escolha o seu plano',
    'subscription.select': 'Selecionar',
    'subscription.recommended': 'Recomendado',

    'subscription.starter.subtitle': 'Starter',
    'subscription.starter.title': 'Grátis',
    'subscription.starter.price': '€0 / mês',
    'subscription.starter.feature1': 'Explorar e pedir itens perto de si',
    'subscription.starter.feature2': 'Enviar mensagens e combinar a recolha',
    'subscription.starter.feature3': 'Verificações básicas de confiança e segurança',
    'subscription.starter.feature4': 'Suporte padrão',

    'subscription.plus.subtitle': 'Plus',
    'subscription.plus.title': 'Verificado',
    'subscription.plus.price': '€4,99 / mês',
    'subscription.plus.trial': 'Teste grátis de 14 dias',
    'subscription.plus.feature1': 'Selo de utilizador verificado',
    'subscription.plus.feature2': 'Aprovação prioritária em alguns anúncios',
    'subscription.plus.feature3': 'Taxas de serviço reduzidas quando aplicável',
    'subscription.plus.feature4': 'Acesso a opções de proteção em itens elegíveis',
    'subscription.plus.feature5': 'Suporte prioritário',

    'subscription.pro.subtitle': 'Pro',
    'subscription.pro.title': 'Premium Lender',
    'subscription.pro.price': 'Em breve',
    'subscription.pro.trial': 'Lançamento limitado',
    'subscription.pro.feature1': 'Mais visibilidade para os seus anúncios',
    'subscription.pro.feature2': 'Análises avançadas',
    'subscription.pro.feature3': 'Pagamentos mais rápidos',
    'subscription.pro.feature4': 'Políticas personalizadas',
    'subscription.pro.feature5': 'Suporte dedicado',
    'subscription.pro.feature6': 'Funcionalidades para equipas/organizações'
  },
  de: {
    'common.back': 'Zurück',
    'common.cancel': 'Abbrechen',
    'common.confirm': 'Bestätigen',
    'common.copied': 'Kopiert',
    'common.error': 'Fehler',
    'common.loading': 'Lädt…',

    'home.hero_title_1': 'Mehr teilen. Weniger besitzen.',
    'home.hero_desc': 'Leihen, verleihen und verschenken in deiner Nachbarschaft — vertrauenswürdig, schnell und lokal.',
    'home.filter_all': 'Alle',
    'home.filter_goods': 'Artikel',
    'home.filter_skills': 'Fähigkeiten',
    'home.category_all': 'Alle Kategorien',

    'nav.discover': 'Entdecken',
    'nav.enterprise': 'Unternehmen',
    'nav.dashboard': 'Dashboard',
    'nav.admin': 'Admin',
    'nav.messages': 'Nachrichten',
    'nav.subscribe': 'Abo',

    'connect.welcome_back': 'Willkommen zurück',
    'connect.create_account_title': 'Konto erstellen',
    'connect.login_help': 'Melde dich an, um fortzufahren.',
    'connect.register_help': 'Erstelle ein Konto, um zu starten.',
    'connect.sign_in': 'Anmelden',
    'connect.register': 'Registrieren',
    'connect.full_name': 'Vollständiger Name',
    'connect.email': 'E-Mail',
    'connect.password': 'Passwort',
    'connect.reset_password': 'Passwort zurücksetzen',
    'connect.create_account_cta': 'Konto erstellen',
    'connect.terms.prefix': 'Mit dem Fortfahren stimmst du unseren',
    'connect.terms.terms': 'AGB',
    'connect.terms.and': 'und der',
    'connect.terms.privacy': 'Datenschutzerklärung zu',
    'connect.demo.trust98': 'Vertrauen 98',
    'connect.demo.unverified': 'Nicht verifiziert',
    'connect.footer_info': 'Du kannst deine Einstellungen später ändern.',
    'connect.privacy.title': 'Datenschutz',
    'connect.privacy.agree': 'Wir nutzen deine Daten nur für den Betrieb des Dienstes.',
    'connect.create_free_account': 'Kostenloses Konto erstellen',
    'connect.trust.title': 'Vertrauens-Score',
    'connect.trust.help': 'Verifizierte Profile sorgen für sichereres Teilen.',
    'connect.hyperlocal.title': 'Hyperlokal',
    'connect.hyperlocal.help': 'Finde, was du brauchst, in deiner Nähe.',
    'connect.privacy.bullet1': 'Kein Spam. Keine Weitergabe deiner Daten.',
    'connect.privacy.bullet2': 'Du entscheidest, was du teilst und mit wem.',
    'connect.privacy.bullet3': 'Verifizierte Nachbarn, sicherer teilen.',


    'lenderSub.loading': 'Lädt…',

    'subscription.back_to_discovery': 'Zurück zur Suche',
    'subscription.title': 'Wähle deinen Plan',
    'subscription.select': 'Auswählen',
    'subscription.recommended': 'Empfohlen',

    'subscription.starter.subtitle': 'Starter',
    'subscription.starter.title': 'Kostenlos',
    'subscription.starter.price': '€0 / Monat',
    'subscription.starter.feature1': 'Artikel in der Nähe finden und anfragen',
    'subscription.starter.feature2': 'Nachrichten senden und Abholung abstimmen',
    'subscription.starter.feature3': 'Grundlegende Vertrauens- und Sicherheitschecks',
    'subscription.starter.feature4': 'Standard-Support',

    'subscription.plus.subtitle': 'Plus',
    'subscription.plus.title': 'Verifiziert',
    'subscription.plus.price': '€4,99 / Monat',
    'subscription.plus.trial': '14 Tage kostenlos testen',
    'subscription.plus.feature1': 'Verifiziertes Profil-Abzeichen',
    'subscription.plus.feature2': 'Priorisierte Freigabe bei einigen Inseraten',
    'subscription.plus.feature3': 'Geringere Servicegebühren (falls zutreffend)',
    'subscription.plus.feature4': 'Schutzoptionen für geeignete Artikel',
    'subscription.plus.feature5': 'Priorisierter Support',

    'subscription.pro.subtitle': 'Pro',
    'subscription.pro.title': 'Premium Lender',
    'subscription.pro.price': 'Demnächst verfügbar',
    'subscription.pro.trial': 'Begrenzter Rollout',
    'subscription.pro.feature1': 'Mehr Sichtbarkeit für deine Inserate',
    'subscription.pro.feature2': 'Erweiterte Analysen',
    'subscription.pro.feature3': 'Schnellere Auszahlungen',
    'subscription.pro.feature4': 'Individuelle Richtlinien',
    'subscription.pro.feature5': 'Dedizierter Support',
    'subscription.pro.feature6': 'Team-/Organisationsfunktionen'
  }
};

@Injectable({
  providedIn: 'root'
})
export class I18nService {
  private languageKey = 'i18n_language';
  private currencyKey = 'i18n_currency';
  private appRef = inject(ApplicationRef);

  language = signal<SupportedLanguage>(this.readLanguage());
  currency = signal<SupportedCurrency>(this.readCurrency());

  supportedCurrencies: SupportedCurrency[] = ['EUR', 'USD', 'GBP', 'BRL'];

  t(key: string): string {
    const k = String(key || '').trim();
    if (!k) return '';
    const lang = this.language();
    return TRANSLATIONS[lang]?.[k] ?? this.humanizeKey(k);
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
    try {
      const el = (globalThis as any)?.document?.documentElement;
      if (el) el.lang = lang;
    } catch { }
    try {
      this.appRef.tick();
    } catch { }
  }

  setCurrency(curr: SupportedCurrency) {
    this.currency.set(curr);
    try {
      localStorage.setItem(this.currencyKey, curr);
    } catch { }
    try {
      this.appRef.tick();
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
    try {
      const nav = String((globalThis as any)?.navigator?.language || '').toLowerCase();
      if (nav.startsWith('pt')) return 'pt';
      if (nav.startsWith('de')) return 'de';
    } catch { }
    return 'en';
  }

  private readCurrency(): SupportedCurrency {
    try {
      const v = String(localStorage.getItem(this.currencyKey) || '').trim().toUpperCase();
      if (v === 'EUR' || v === 'USD' || v === 'GBP' || v === 'BRL') return v as any;
    } catch { }
    return 'EUR';
  }

  private humanizeKey(key: string): string {
    const raw = String(key || '').trim();
    if (!raw) return '';
    const last = raw.split('.').filter(Boolean).pop() || raw;
    const spaced = last
      .replace(/_/g, ' ')
      .replace(/([a-z])([A-Z])/g, '$1 $2')
      .replace(/\s+/g, ' ')
      .trim();
    if (!spaced) return raw;
    const words = spaced.split(' ').map(w => {
      const lw = w.toLowerCase();
      if (lw === '2fa') return '2FA';
      if (lw === 'gdpr') return 'GDPR';
      if (lw === 'id') return 'ID';
      if (lw === 'mfa') return 'MFA';
      if (lw === 'api') return 'API';
      return lw.charAt(0).toUpperCase() + lw.slice(1);
    });
    if (words.length === 0) return raw;
    words[0] = words[0].charAt(0).toUpperCase() + words[0].slice(1);
    return words.join(' ');
  }
}

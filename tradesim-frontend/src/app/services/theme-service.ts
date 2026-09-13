import { Injectable, inject, PLATFORM_ID } from '@angular/core';
import { DOCUMENT, isPlatformBrowser } from '@angular/common';

@Injectable({
  providedIn: 'root'
})
export class ThemeService {
  private readonly document = inject(DOCUMENT);
  private readonly platformId = inject(PLATFORM_ID);
  private mediaQueryList: MediaQueryList | null = null;

  constructor() {
    if (isPlatformBrowser(this.platformId)) {
      this.mediaQueryList = window.matchMedia('(prefers-color-scheme: dark)');
      this.mediaQueryList.addEventListener('change', this.onSystemThemeChange);

      const savedTheme = localStorage.getItem('tradesim-theme') || 'SYSTEM';
      this.applyTheme(savedTheme);
    }
  }

  setTheme(theme: string) {
    if (isPlatformBrowser(this.platformId)) {
      localStorage.setItem('tradesim-theme', theme);
      this.applyTheme(theme);
    }
  }

  private onSystemThemeChange = () => {
    const savedTheme = localStorage.getItem('tradesim-theme') || 'SYSTEM';
    if (savedTheme === 'SYSTEM') {
      this.applyTheme('SYSTEM');
    }
  };

  private applyTheme(theme: string) {
    let isDark = false;

    if (theme === 'SYSTEM') {
      isDark = this.mediaQueryList?.matches ?? false;
    } else {
      isDark = theme === 'DARK';
    }

    if (isDark) {
      this.document.documentElement.setAttribute('data-theme', 'dark');
    } else {
      this.document.documentElement.removeAttribute('data-theme');
    }
  }
}
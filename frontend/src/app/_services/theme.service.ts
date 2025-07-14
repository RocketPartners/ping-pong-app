import {Injectable, Renderer2, RendererFactory2} from '@angular/core';
import {BehaviorSubject, Observable} from 'rxjs';

export type ThemeMode = 'light' | 'dark';

@Injectable({
  providedIn: 'root'
})
export class ThemeService {
  private renderer: Renderer2;
  private currentThemeSubject = new BehaviorSubject<ThemeMode>(this.getStoredTheme());
  public readonly currentTheme$: Observable<ThemeMode> = this.currentThemeSubject.asObservable();
  private customColorSubject = new BehaviorSubject<string | null>(this.getStoredCustomColor());
  public readonly customColor$: Observable<string | null> = this.customColorSubject.asObservable();

  constructor(rendererFactory: RendererFactory2) {
    this.renderer = rendererFactory.createRenderer(null, null);
    this.initializeTheme();
    this.listenForSystemThemeChanges();
  }

  public setTheme(theme: ThemeMode): void {
    this.currentThemeSubject.next(theme);
    localStorage.setItem('theme', theme);
    this.applyTheme(theme);
  }

  public toggleTheme(): void {
    const newTheme = this.currentThemeSubject.value === 'light' ? 'dark' : 'light';
    this.setTheme(newTheme);
  }

  /**
   * Set a custom primary color for the user's theme
   * @param color - Hex color code (e.g., '#1976d2')
   */
  public setCustomPrimaryColor(color: string | null): void {
    this.customColorSubject.next(color);

    if (color) {
      localStorage.setItem('customPrimaryColor', color);
    } else {
      localStorage.removeItem('customPrimaryColor');
    }

    this.applyCustomColor(color);
  }

  /**
   * Reset custom color back to the default theme color
   */
  public resetCustomColor(): void {
    this.setCustomPrimaryColor(null);
  }

  private getStoredTheme(): ThemeMode {
    const storedTheme = localStorage.getItem('theme') as ThemeMode;
    if (storedTheme && (storedTheme === 'light' || storedTheme === 'dark')) {
      return storedTheme;
    }

    // Check for system preference with a more robust approach
    if (window.matchMedia) {
      try {
        // Check for dark mode preference
        if (window.matchMedia('(prefers-color-scheme: dark)').matches) {
          return 'dark';
        }
        
        // Check for light mode preference
        if (window.matchMedia('(prefers-color-scheme: light)').matches) {
          return 'light';
        }
        
        // If the browser supports the API but no preference is set
        console.info('Browser supports prefers-color-scheme but no preference detected');
      } catch (error) {
        console.warn('Error checking color scheme preference:', error);
      }
    }

    return 'light'; // Default theme when no preference can be determined
  }
  
  /**
   * Listen for system theme preference changes
   * @private
   */
  private listenForSystemThemeChanges(): void {
    if (window.matchMedia) {
      const darkModeMediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
      
      // Modern browsers support addEventListener
      if (darkModeMediaQuery.addEventListener) {
        darkModeMediaQuery.addEventListener('change', (e) => {
          // Only apply if the user hasn't manually set a theme
          if (!localStorage.getItem('theme')) {
            this.setTheme(e.matches ? 'dark' : 'light');
          }
        });
      } else if (darkModeMediaQuery.addListener) {
        // Older browsers support addListener (deprecated)
        darkModeMediaQuery.addListener((e) => {
          // Only apply if the user hasn't manually set a theme
          if (!localStorage.getItem('theme')) {
            this.setTheme(e.matches ? 'dark' : 'light');
          }
        });
      }
    }
  }

  private getStoredCustomColor(): string | null {
    return localStorage.getItem('customPrimaryColor');
  }

  private initializeTheme(): void {
    const theme = this.currentThemeSubject.value;
    this.applyTheme(theme);

    // Also apply custom color if one exists
    const customColor = this.customColorSubject.value;
    if (customColor) {
      this.applyCustomColor(customColor);
    }
  }

  private applyTheme(theme: ThemeMode): void {
    // Apply class to the body
    this.renderer.removeClass(document.body, 'light-theme');
    this.renderer.removeClass(document.body, 'dark-theme');
    this.renderer.addClass(document.body, `${theme}-theme`);

    // Also apply to the html element to ensure Angular Material picks it up
    this.renderer.removeClass(document.documentElement, 'light-theme');
    this.renderer.removeClass(document.documentElement, 'dark-theme');
    this.renderer.addClass(document.documentElement, `${theme}-theme`);
  }

  private applyCustomColor(color: string | null): void {
    // Remove existing style element if there is one
    const existingStyle = document.getElementById('custom-primary-color');
    if (existingStyle) {
      existingStyle.remove();
    }

    if (!color) {
      return; // If no color provided, just remove the custom styles
    }

    // Create a lighter and darker shade for the primary color
    const lighterColor = this.lightenColor(color, 20);
    const darkerColor = this.darkenColor(color, 20);

    // Convert hex to rgb
    const rgbColor = this.hexToRgb(color);

    // Create a style element
    const style = document.createElement('style');
    style.id = 'custom-primary-color';
    style.textContent = `
      :root {
        --primary-color: ${color};
        --primary-color-light: ${lighterColor};
        --primary-color-dark: ${darkerColor};
        --primary-color-rgb: ${rgbColor};
      }
    `;

    // Add it to the head
    document.head.appendChild(style);
  }

  /**
   * Lightens a color by the given percentage
   */
  private lightenColor(hex: string, percent: number): string {
    // Convert to RGB
    const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
    if (!result) return hex;

    // Calculate HSL (Hue, Saturation, Lightness) which is better for adjusting brightness
    const r = parseInt(result[1], 16) / 255;
    const g = parseInt(result[2], 16) / 255;
    const b = parseInt(result[3], 16) / 255;
    
    const max = Math.max(r, g, b);
    const min = Math.min(r, g, b);
    let h = 0;
    let s = 0;
    const l = (max + min) / 2;

    if (max !== min) {
      const d = max - min;
      s = l > 0.5 ? d / (2 - max - min) : d / (max + min);
      
      if (max === r) {
        h = (g - b) / d + (g < b ? 6 : 0);
      } else if (max === g) {
        h = (b - r) / d + 2;
      } else if (max === b) {
        h = (r - g) / d + 4;
      }
      
      h /= 6;
    }

    // Adjust lightness
    const newL = Math.min(1, l + (percent / 100) * (1 - l));
    
    // Convert back to RGB
    return this.hslToHex(h, s, newL);
  }

  /**
   * Darkens a color by the given percentage
   */
  private darkenColor(hex: string, percent: number): string {
    // Convert to RGB
    const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
    if (!result) return hex;

    // Calculate HSL (Hue, Saturation, Lightness) which is better for adjusting brightness
    const r = parseInt(result[1], 16) / 255;
    const g = parseInt(result[2], 16) / 255;
    const b = parseInt(result[3], 16) / 255;
    
    const max = Math.max(r, g, b);
    const min = Math.min(r, g, b);
    let h = 0;
    let s = 0;
    const l = (max + min) / 2;

    if (max !== min) {
      const d = max - min;
      s = l > 0.5 ? d / (2 - max - min) : d / (max + min);
      
      if (max === r) {
        h = (g - b) / d + (g < b ? 6 : 0);
      } else if (max === g) {
        h = (b - r) / d + 2;
      } else if (max === b) {
        h = (r - g) / d + 4;
      }
      
      h /= 6;
    }

    // Adjust lightness
    const newL = Math.max(0, l - (percent / 100) * l);
    
    // Convert back to RGB
    return this.hslToHex(h, s, newL);
  }
  
  /**
   * Convert HSL to Hex
   */
  private hslToHex(h: number, s: number, l: number): string {
    let r, g, b;

    if (s === 0) {
      r = g = b = l; // achromatic
    } else {
      const hue2rgb = (p: number, q: number, t: number) => {
        if (t < 0) t += 1;
        if (t > 1) t -= 1;
        if (t < 1/6) return p + (q - p) * 6 * t;
        if (t < 1/2) return q;
        if (t < 2/3) return p + (q - p) * (2/3 - t) * 6;
        return p;
      };

      const q = l < 0.5 ? l * (1 + s) : l + s - l * s;
      const p = 2 * l - q;
      
      r = hue2rgb(p, q, h + 1/3);
      g = hue2rgb(p, q, h);
      b = hue2rgb(p, q, h - 1/3);
    }

    // Convert to hex
    const toHex = (x: number) => {
      const hex = Math.round(x * 255).toString(16);
      return hex.length === 1 ? '0' + hex : hex;
    };
    
    return `#${toHex(r)}${toHex(g)}${toHex(b)}`;
  }

  /**
   * Convert hex color to RGB string
   */
  private hexToRgb(hex: string): string {
    // Convert to RGB
    const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
    if (!result) return '0, 0, 0';

    const r = parseInt(result[1], 16);
    const g = parseInt(result[2], 16);
    const b = parseInt(result[3], 16);

    return `${r}, ${g}, ${b}`;
  }
}

import { Component, EventEmitter, HostBinding, Input, OnInit, Output, ElementRef, Renderer2 } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LucideAngularModule, Loader2 } from 'lucide-angular';

export type ButtonVariant = 'primary' | 'secondary' | 'tertiary' | 'destructive' | 'dark';
export type ButtonSize = 'sm' | 'md' | 'lg';

@Component({
  selector: 'app-button',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  template: `
    <button
      [disabled]="disabled || isLoading"
      (click)="onClick.emit($event)"
      [class]="computedClasses"
    >
      <lucide-icon *ngIf="isLoading" [img]="Loader2" class="animate-spin" [size]="size === 'sm' ? 14 : 18"></lucide-icon>
      <ng-content></ng-content>
    </button>
  `,
})
export class ButtonComponent {
  @Input() variant: ButtonVariant = 'primary';
  @Input() size: ButtonSize = 'md';
  @Input() isLoading = false;
  @Input() fullWidth = false;
  @Input() disabled = false;
  @Input() className = '';

  @Output() onClick = new EventEmitter<MouseEvent>();

  readonly Loader2 = Loader2;

  @HostBinding('style.display') hostDisplay = 'contents';

  private hostClasses = '';

  constructor(private host: ElementRef<HTMLElement>, private renderer: Renderer2) {}

  ngOnInit(): void {
    const cls = this.host.nativeElement.getAttribute('class') || '';
    this.hostClasses = cls;
    if (cls) {
      this.renderer.setAttribute(this.host.nativeElement, 'class', '');
    }
  }

  get computedClasses(): string {
    const baseStyles = 'inline-flex items-center justify-center font-semibold transition-colors focus:outline-none focus:ring-2 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed rounded-xl';
    
    const variants: Record<ButtonVariant, string> = {
      primary: 'bg-teal-600 text-white hover:bg-teal-700 focus:ring-teal-500 shadow-sm',
      secondary: 'bg-white text-gray-700 border border-gray-300 hover:bg-gray-50 focus:ring-gray-500 shadow-sm',
      tertiary: 'text-gray-600 hover:text-gray-900 hover:bg-gray-100 focus:ring-gray-500',
      destructive: 'bg-red-600 text-white hover:bg-red-700 focus:ring-red-500 shadow-sm',
      dark: 'bg-gray-900 text-white hover:bg-gray-800 focus:ring-gray-900 shadow-sm',
    };

    const sizes: Record<ButtonSize, string> = {
      sm: 'text-xs px-3 py-1.5 gap-1.5',
      md: 'text-sm px-4 py-2.5 gap-2',
      lg: 'text-base px-6 py-3 gap-2.5',
    };

    const widthClass = this.fullWidth ? 'w-full' : '';

    return `${baseStyles} ${variants[this.variant]} ${sizes[this.size]} ${widthClass} ${this.hostClasses} ${this.className}`;
  }
}

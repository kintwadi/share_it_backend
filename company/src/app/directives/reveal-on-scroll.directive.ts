import { Directive, ElementRef, OnDestroy, OnInit, Renderer2, inject } from '@angular/core';

@Directive({
  selector: '[appRevealOnScroll]',
  standalone: true
})
export class RevealOnScrollDirective implements OnInit, OnDestroy {
  private readonly elementRef = inject(ElementRef<HTMLElement>);
  private readonly renderer = inject(Renderer2);
  private observer?: IntersectionObserver;

  ngOnInit(): void {
    this.renderer.addClass(this.elementRef.nativeElement, 'fade-in');

    if (typeof window === 'undefined' || typeof IntersectionObserver === 'undefined') {
      this.renderer.addClass(this.elementRef.nativeElement, 'visible');
      return;
    }

    this.observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            this.renderer.addClass(this.elementRef.nativeElement, 'visible');
            this.observer?.unobserve(entry.target);
          }
        });
      },
      {
        threshold: 0.16,
        rootMargin: '0px 0px -48px 0px'
      }
    );

    this.observer.observe(this.elementRef.nativeElement);
  }

  ngOnDestroy(): void {
    this.observer?.disconnect();
  }
}

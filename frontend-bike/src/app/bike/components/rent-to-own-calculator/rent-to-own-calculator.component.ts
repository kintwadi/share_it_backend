import { CommonModule, CurrencyPipe } from '@angular/common';
import { ChangeDetectorRef, Component, Input, OnChanges, SimpleChanges, inject } from '@angular/core';
import { BikeApiService } from '../../services/bike-api.service';
import { RentToOwnQuote } from '../../models/bike';

@Component({
  selector: 'app-rent-to-own-calculator',
  standalone: true,
  imports: [CommonModule, CurrencyPipe],
  templateUrl: './rent-to-own-calculator.component.html',
  styleUrl: './rent-to-own-calculator.component.css'
})
export class RentToOwnCalculatorComponent implements OnChanges {
  @Input() bikeId = '';
  @Input() retailPurchasePrice: number | null = null;
  @Input() rentToOwnEligible = false;

  private bikeApi = inject(BikeApiService);
  private cdr = inject(ChangeDetectorRef);

  loading = false;
  quote: RentToOwnQuote | null = null;
  error = '';

  private render(): void {
    try {
      this.cdr.detectChanges();
    } catch {
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['bikeId'] && this.bikeId && this.rentToOwnEligible) {
      void this.loadQuote();
    }
  }

  async loadQuote(): Promise<void> {
    this.loading = true;
    this.error = '';
    this.render();
    try {
      this.quote = await this.bikeApi.getRentToOwnQuote(this.bikeId);
    } catch {
      this.quote = null;
      this.error = 'Rent-to-own quote is unavailable right now.';
    } finally {
      this.loading = false;
      this.render();
    }
  }
}

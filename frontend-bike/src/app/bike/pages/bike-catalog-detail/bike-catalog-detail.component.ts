import { CommonModule, CurrencyPipe } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { BikeShopDetail } from '../../models/bike';
import { BikeApiService } from '../../services/bike-api.service';
import { formatBikeEnumLabel } from '../../utils/bike-labels';

@Component({
  selector: 'app-bike-catalog-detail',
  standalone: true,
  imports: [CommonModule, RouterLink, CurrencyPipe],
  templateUrl: './bike-catalog-detail.component.html',
  styleUrl: './bike-catalog-detail.component.css'
})
export class BikeCatalogDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly bikeApi = inject(BikeApiService);
  private readonly cdr = inject(ChangeDetectorRef);

  bike: BikeShopDetail | null = null;
  loading = true;
  error = '';

  async ngOnInit(): Promise<void> {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.error = 'Bike not found.';
      this.loading = false;
      this.render();
      return;
    }

    try {
      this.bike = await this.bikeApi.getShopBike(id);
    } catch {
      this.error = 'Unable to load the bicycle catalog item.';
    } finally {
      this.loading = false;
      this.render();
    }
  }

  formatEnum(value: string | null | undefined): string {
    return formatBikeEnumLabel(value);
  }

  private render(): void {
    try {
      this.cdr.detectChanges();
    } catch {
    }
  }
}

import { CommonModule, CurrencyPipe } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { HandoverChecklistComponent } from '../../components/handover-checklist/handover-checklist.component';
import { RentToOwnCalculatorComponent } from '../../components/rent-to-own-calculator/rent-to-own-calculator.component';
import { BikeDetail } from '../../models/bike';
import { BikeApiService } from '../../services/bike-api.service';

@Component({
  selector: 'app-bike-detail',
  standalone: true,
  imports: [CommonModule, RouterLink, CurrencyPipe, HandoverChecklistComponent, RentToOwnCalculatorComponent],
  templateUrl: './bike-detail.component.html',
  styleUrl: './bike-detail.component.css'
})
export class BikeDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private bikeApi = inject(BikeApiService);
  private cdr = inject(ChangeDetectorRef);

  bike: BikeDetail | null = null;
  loading = true;
  error = '';

  private render(): void {
    try {
      this.cdr.detectChanges();
    } catch {
    }
  }

  async ngOnInit(): Promise<void> {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.error = 'Bike not found.';
      this.loading = false;
      this.render();
      return;
    }

    try {
      this.bike = await this.bikeApi.getById(id);
    } catch {
      this.error = 'Unable to load bicycle details.';
    } finally {
      this.loading = false;
      this.render();
    }
  }
}

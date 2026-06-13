import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { BikeCatalogItem } from '../../bike/models/bike';
import { BikeApiService } from '../../bike/services/bike-api.service';
import { BikeCardComponent } from '../../shared/components/bike-card.component';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterLink, BikeCardComponent],
  templateUrl: './home.component.html',
  styleUrl: './home.component.css'
})
export class HomeComponent implements OnInit {
  private readonly bikeApi = inject(BikeApiService);

  featuredBikes: BikeCatalogItem[] = [];
  loading = true;

  async ngOnInit(): Promise<void> {
    try {
      const page = await this.bikeApi.search({ page: 0, size: 3 });
      this.featuredBikes = page.content ?? [];
    } finally {
      this.loading = false;
    }
  }
}

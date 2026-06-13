import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { BikeCatalogItem } from '../../models/bike';
import { BikeApiService } from '../../services/bike-api.service';

@Component({
  selector: 'app-bike-search',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './bike-search.component.html',
  styleUrl: './bike-search.component.css'
})
export class BikeSearchComponent implements OnInit {
  private bikeApi = inject(BikeApiService);
  private cdr = inject(ChangeDetectorRef);

  bikes: BikeCatalogItem[] = [];
  search = '';
  frameSize = '';
  bikeType = '';
  loading = true;

  private render(): void {
    try {
      this.cdr.detectChanges();
    } catch {
    }
  }

  async ngOnInit(): Promise<void> {
    await this.load();
  }

  async load(): Promise<void> {
    this.loading = true;
    this.render();
    try {
      const page = await this.bikeApi.search({
        search: this.search,
        frameSize: this.frameSize,
        bikeType: this.bikeType,
        page: 0,
        size: 24
      });
      this.bikes = page.content ?? [];
    } finally {
      this.loading = false;
      this.render();
    }
  }
}

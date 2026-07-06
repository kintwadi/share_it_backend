import { CommonModule, CurrencyPipe } from '@angular/common';
import { Component, Input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { BikeCatalogItem } from '../../bike/models/bike';

@Component({
  selector: 'app-bike-card',
  standalone: true,
  imports: [CommonModule, RouterLink, CurrencyPipe],
  templateUrl: './bike-card.component.html',
  styleUrl: './bike-card.component.css'
})
export class BikeCardComponent {
  @Input({ required: true }) bike!: BikeCatalogItem;
}

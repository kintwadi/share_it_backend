import { CommonModule, CurrencyPipe } from '@angular/common';
import { Component, Input } from '@angular/core';
import { RouterLink } from '@angular/router';
import {
  extractManufacturer,
  extractModelLine,
  resolveBrandTheme
} from '../fahrrad-fuchs-to-go-branding';
import { FahrradFuchsToGoCatalogItem } from '../fahrrad-fuchs-to-go.models';
import { TPipe } from '../../core/i18n/t.pipe';

@Component({
  selector: 'app-fahrrad-fuchs-to-go-bike-card',
  standalone: true,
  imports: [CommonModule, RouterLink, CurrencyPipe, TPipe],
  templateUrl: './fahrrad-fuchs-to-go-bike-card.component.html',
  styleUrl: './fahrrad-fuchs-to-go-bike-card.component.css'
})
export class FahrradFuchsToGoBikeCardComponent {
  @Input({ required: true }) bike!: FahrradFuchsToGoCatalogItem;

  get manufacturer(): string {
    return extractManufacturer(this.bike.title, this.bike.category);
  }

  get modelLine(): string {
    return extractModelLine(this.bike.title) || this.bike.title;
  }

  get theme() {
    return resolveBrandTheme(this.bike.title);
  }
}

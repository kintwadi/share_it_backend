import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { BikeFooterComponent } from './shared/components/bike-footer.component';
import { BikeHeaderComponent } from './shared/components/bike-header.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, BikeHeaderComponent, BikeFooterComponent],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {}

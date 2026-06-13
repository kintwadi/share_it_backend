import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-bike-footer',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './bike-footer.component.html',
  styleUrl: './bike-footer.component.css'
})
export class BikeFooterComponent {}

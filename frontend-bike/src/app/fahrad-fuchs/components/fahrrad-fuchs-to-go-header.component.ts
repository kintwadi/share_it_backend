import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FahrradFuchsToGoStore } from '../fahrrad-fuchs-to-go.models';

@Component({
  selector: 'app-fahrrad-fuchs-to-go-header',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './fahrrad-fuchs-to-go-header.component.html',
  styleUrl: './fahrrad-fuchs-to-go-header.component.css'
})
export class FahrradFuchsToGoHeaderComponent {
  @Input({ required: true }) store!: FahrradFuchsToGoStore;
}

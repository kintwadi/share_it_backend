import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { FahrradFuchsToGoStore } from '../fahrrad-fuchs-to-go.models';

@Component({
  selector: 'app-fahrrad-fuchs-to-go-footer',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './fahrrad-fuchs-to-go-footer.component.html',
  styleUrl: './fahrrad-fuchs-to-go-footer.component.css'
})
export class FahrradFuchsToGoFooterComponent {
  @Input({ required: true }) store!: FahrradFuchsToGoStore;
}

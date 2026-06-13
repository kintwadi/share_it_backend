import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { FormsModule } from '@angular/forms';

interface ChecklistItem {
  key: string;
  label: string;
  done: boolean;
}

@Component({
  selector: 'app-handover-checklist',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './handover-checklist.component.html',
  styleUrl: './handover-checklist.component.css'
})
export class HandoverChecklistComponent {
  @Input() bikeTitle = '';

  signature = '';
  checklist: ChecklistItem[] = [
    { key: 'frontBrake', label: 'Front brake tested', done: false },
    { key: 'rearBrake', label: 'Rear brake tested', done: false },
    { key: 'tires', label: 'Tires and pressure checked', done: false },
    { key: 'stemTorque', label: 'Stem bolt torque verified', done: false },
    { key: 'controller', label: 'Electric controller status confirmed', done: false }
  ];

  get completedCount(): number {
    return this.checklist.filter(item => item.done).length;
  }
}

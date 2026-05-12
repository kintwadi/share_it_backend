import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { CommonModule } from '@angular/common';
import { Layout } from './core/layout/layout/layout';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, Layout],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent {}

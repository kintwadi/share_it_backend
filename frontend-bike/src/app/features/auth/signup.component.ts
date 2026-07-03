import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-signup',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './signup.component.html',
  styleUrl: './auth-shell.css'
})
export class SignupComponent {
  private readonly auth = inject(AuthService);

  name = '';
  email = '';
  password = '';
  phone = '';
  address = '';
  loading = false;
  success = '';
  error = '';

  async submit(): Promise<void> {
    this.loading = true;
    this.error = '';
    this.success = '';
    try {
      await this.auth.register({
        name: this.name,
        email: this.email,
        password: this.password,
        phone: this.phone,
        address: this.address
      });
      this.success = 'Account created. You can now log in to the bike storefront.';
    } catch {
      this.error = 'Unable to create your bike account right now.';
    } finally {
      this.loading = false;
    }
  }
}

import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { LucideAngularModule, ArrowLeft, MessageSquare, User as UserIcon } from 'lucide-angular';
import { User } from '../../core/models/types';
import { I18nService } from '../../core/services/i18n.service';

@Component({
  selector: 'app-user-preview',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  templateUrl: './user-preview.component.html',
  styleUrl: './user-preview.component.css'
})
export class UserPreviewComponent {
  private router = inject(Router);
  i18n = inject(I18nService);

  readonly ArrowLeft = ArrowLeft;
  readonly MessageSquare = MessageSquare;
  readonly UserIcon = UserIcon;

  user: User | null = (history.state && (history.state as any).user) ? ((history.state as any).user as User) : null;
  returnTo: string = (history.state && typeof (history.state as any).returnTo === 'string') ? String((history.state as any).returnTo) : '/dashboard';

  back() {
    this.router.navigateByUrl(this.returnTo || '/dashboard');
  }

  message() {
    const u = this.user;
    if (!u) return;
    const email = String((u as any).email || '').trim();
    const id = String((u as any).id || '').trim();
    const qp: any = {};
    if (email) qp.receiverEmail = email;
    else if (id) qp.receiverId = id;
    this.router.navigate(['/mailbox/compose'], { queryParams: qp, state: { returnTo: this.returnTo || '/dashboard' } as any });
  }
}


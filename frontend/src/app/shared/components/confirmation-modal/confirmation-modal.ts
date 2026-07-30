import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LucideAngularModule, X, AlertTriangle, Info } from 'lucide-angular';
import { I18nService } from '../../../core/services/i18n.service';
import { LayoutModeService } from '../../../core/services/layout-mode.service';

@Component({
  selector: 'app-confirmation-modal',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  templateUrl: './confirmation-modal.html',
  styleUrl: './confirmation-modal.css'
})
export class ConfirmationModalComponent {
  private i18n = inject(I18nService);
  layoutMode = inject(LayoutModeService);
  @Input() isOpen = false;
  @Input() title = '';
  @Input() message = '';
  @Input() confirmLabel = this.i18n.t('common.confirm');
  @Input() cancelLabel = this.i18n.t('common.cancel');
  @Input() confirmDisabled = false;
  @Input() variant: 'danger' | 'warning' | 'info' = 'info';
  @Output() cancel = new EventEmitter<void>();
  @Output() confirm = new EventEmitter<void>();

  readonly X = X;
  readonly AlertTriangle = AlertTriangle;
  readonly Info = Info;

  onCancel() {
    this.cancel.emit();
  }

  onConfirm() {
    if (this.confirmDisabled) return;
    this.confirm.emit();
  }
}

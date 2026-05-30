import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PartnerSubmitListingComponent } from '../partner-submit-listing/partner-submit-listing.component';
import { PartnerReturnRequestsComponent } from '../partner-return-requests/partner-return-requests.component';

@Component({
  selector: 'app-partner-dashboard',
  standalone: true,
  imports: [CommonModule, PartnerReturnRequestsComponent, PartnerSubmitListingComponent],
  templateUrl: './partner-dashboard.component.html',
  styleUrl: './partner-dashboard.component.css'
})
export class PartnerDashboardComponent {
}

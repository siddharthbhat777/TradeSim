import { ChangeDetectionStrategy, Component, input, model, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Modal } from '../../../../../shared/components/modal/modal';
import { Button } from '../../../../../shared/components/button/button';
import { Badge } from '../../../../../shared/components/badge/badge';
import { Skeleton } from '../../../../../shared/components/loaders/skeleton/skeleton';
import { OrderEstimateResponse } from '../../../../../models/order';
import { OrderTicketPayload } from '../order-ticket/order-ticket';

@Component({
  selector: 'app-order-review-modal',
  imports: [CommonModule, Modal, Button, Badge, Skeleton],
  templateUrl: './order-review-modal.html',
  styleUrl: './order-review-modal.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class OrderReviewModal {
  readonly isOpen = model<boolean>(false);
  readonly payload = input.required<OrderTicketPayload | null>();
  readonly stockSymbol = input.required<string>();
  readonly baseCurrency = input.required<string>();
  readonly isEstimatingOrder = input.required<boolean>();
  readonly orderEstimate = input.required<OrderEstimateResponse | null>();
  readonly isFetchingRate = input.required<boolean>();
  readonly isSubmittingOrder = input.required<boolean>();

  readonly openDeposit = output<void>();
  readonly executeOrder = output<void>();
}
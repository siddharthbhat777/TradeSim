import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Logo } from '../shared/components/logo/logo';
import { ToastService } from '../shared/components/toast/toast.service';
import { Button } from '../shared/components/button/button';
import { Badge } from '../shared/components/badge/badge';
import { Card } from '../shared/components/card/card';
import { Alert } from '../shared/components/alert/alert';
import { Tooltip } from '../shared/components/tooltip/tooltip';
import { CustomInput } from '../shared/components/input/input';
import { InputDirective } from '../shared/directives/input';
import { FormsModule } from '@angular/forms';
import { Checkbox } from '../shared/components/checkbox/checkbox';
import { CheckboxGroup } from '../shared/components/checkbox/checkbox-group/checkbox-group';
import { Toggle } from '../shared/components/toggle/toggle';
import { Dropdown, DropdownOption } from '../shared/components/dropdown/dropdown';
import { SegmentedControl, SegmentOption } from '../shared/components/segmented-control/segmented-control';
import { NumberStepper } from '../shared/components/number-stepper/number-stepper';
import { EmptyState } from '../shared/components/empty-state/empty-state';
import { InlineLoader } from '../shared/components/loaders/inline-loader/inline-loader';
import { Loader } from '../shared/components/loaders/loader/loader';
import { Skeleton } from '../shared/components/loaders/skeleton/skeleton';
import { DialogService } from '../shared/components/dialog/dialog.service';

interface DocSection {
  title: string;
  items: { id: string; name: string }[];
}

@Component({
  selector: 'app-testing-playground',
  imports: [CommonModule, FormsModule, Logo, Button, Badge, Card, Alert, Tooltip, CustomInput, InputDirective, Checkbox, CheckboxGroup, Toggle, Dropdown, SegmentedControl, NumberStepper, EmptyState, InlineLoader, Loader, Skeleton],
  templateUrl: './testing-playground.html',
  styleUrls: ['./testing-playground.scss']
})
export class TestingPlayground {
  private toastService = inject(ToastService);
  protected dialogService = inject(DialogService);

  navigation: DocSection[] = [
    {
      title: 'Foundations & Branding',
      items: [
        { id: 'logo', name: 'Logo' },
        { id: 'colors', name: 'Color Palette' },
        { id: 'typography', name: 'Typography' }
      ]
    },
    {
      title: 'Core Components',
      items: [
        { id: 'button', name: 'Button' },
        { id: 'badge', name: 'Badge' },
        { id: 'card', name: 'Card' }
      ]
    },
    {
      title: 'Forms & Controls',
      items: [
        { id: 'input', name: 'Input' },
        { id: 'checkbox', name: 'Checkbox' },
        { id: 'toggle', name: 'Toggle' },
        { id: 'dropdown', name: 'Dropdown' },
        { id: 'segmented-control', name: 'Segmented Control' },
        { id: 'number-stepper', name: 'Number Stepper' }
      ]
    },
    {
      title: 'Feedback & States',
      items: [
        { id: 'alert', name: 'Alert' },
        { id: 'toast', name: 'Toast' },
        { id: 'empty-state', name: 'Empty State' },
        { id: 'loaders', name: 'Loaders' }
      ]
    },
    {
      title: 'Overlays & Popups',
      items: [
        { id: 'dialog', name: 'Dialog' },
        { id: 'tooltip', name: 'Tooltip' }
      ]
    },
    {
      title: 'Data Display',
      items: [
        { id: 'table', name: 'Table' },
        { id: 'pagination', name: 'Pagination' },
        { id: 'price-indicator', name: 'Price Indicator' }
      ]
    },
    {
      title: 'Charts & Visualizations',
      items: [
        { id: 'pie-chart', name: 'Pie Chart' },
        { id: 'legend', name: 'Legend' }
      ]
    },
    {
      title: 'Utilities & Pipes',
      items: [
        { id: 'time-ago', name: 'Time Ago Pipe' }
      ]
    }
  ];

  colorGroups = [
    {
      group: 'Background & Surface',
      tokens: [
        { name: 'Background', variable: '--background' },
        { name: 'Surface', variable: '--surface' },
        { name: 'Surface Sec', variable: '--surface-secondary' },
      ]
    },
    {
      group: 'Borders & Outlines',
      tokens: [
        { name: 'Border', variable: '--border' },
        { name: 'Outline BG', variable: '--outline-bg' },
        { name: 'Outline Border', variable: '--outline-border' },
      ]
    },
    {
      group: 'Brand Actions',
      tokens: [
        { name: 'Primary', variable: '--primary' },
        { name: 'Secondary', variable: '--secondary' },
        { name: 'Accent', variable: '--accent' },
      ]
    },
    {
      group: 'Semantic / Feedback',
      tokens: [
        { name: 'Success', variable: '--success' },
        { name: 'Danger', variable: '--danger' },
        { name: 'Warning', variable: '--warning' },
      ]
    },
    {
      group: 'Charts',
      tokens: [
        { name: 'Chart 1', variable: '--chart-1' },
        { name: 'Chart 2', variable: '--chart-2' },
        { name: 'Chart 3', variable: '--chart-3' },
        { name: 'Chart 4', variable: '--chart-4' },
        { name: 'Chart 5', variable: '--chart-5' },
      ]
    }
  ];

  logoCodeSnippet = `<app-logo size="large" [showText]="true"></app-logo>`;

  dummyEmail = '';
  dummyNotes = '';

  singleCheckbox = false;
  checkboxIndeterminate = true;

  permissions = [
    { id: 'create', name: 'Create Documents' },
    { id: 'read', name: 'Read Documents' },
    { id: 'update', name: 'Update Documents' },
    { id: 'delete', name: 'Delete Documents' }
  ];

  selectedPermissions = ['read'];

  toggleBasic = false;
  toggleSmall = false;
  toggleLarge = true;
  toggleSuccess = true;
  toggleDelete = true;
  toggleCustom = false;

  frameworkOptions: DropdownOption[] = [
    { label: 'Angular', value: 'angular' },
    { label: 'React', value: 'react' },
    { label: 'Vue', value: 'vue' },
    { label: 'Svelte', value: 'svelte' },
    { label: 'Solid', value: 'solid', disabled: true }
  ];
  selectedFramework = null;

  countryOptions: DropdownOption[] = [
    { label: 'United States', value: 'us', icon: '🇺🇸' },
    { label: 'United Kingdom', value: 'uk', icon: '🇬🇧' },
    { label: 'Canada', value: 'ca', icon: '🇨🇦' },
    { label: 'Australia', value: 'au', icon: '🇦🇺' },
    { label: 'Germany', value: 'de', icon: '🇩🇪' },
    { label: 'Japan', value: 'jp', icon: '🇯🇵' },
    { label: 'India', value: 'in', icon: '🇮🇳' }
  ];
  selectedCountry = null;

  viewOptions: SegmentOption[] = [
    { label: 'Map', value: 'map' },
    { label: 'Transit', value: 'transit' },
    { label: 'Satellite', value: 'satellite' }
  ];
  selectedView = 'map';

  sizeOptions: SegmentOption[] = [
    { label: 'Daily', value: 'daily' },
    { label: 'Weekly', value: 'weekly' },
    { label: 'Monthly', value: 'monthly' },
    { label: 'Yearly', value: 'yearly' }
  ];
  selectedFrequency = 'weekly';

  planOptions: SegmentOption[] = [
    { label: 'Free', value: 'free' },
    { label: 'Pro (Locked)', value: 'pro', disabled: true },
    { label: 'Enterprise', value: 'ent' }
  ];
  selectedPlan = 'free';

  stepperBasic = 0;
  stepperLimited = 5;
  stepperLarge = 10;
  stepperMixed = 0;

  showFullScreenLoader = false;

  triggerFullScreenLoader(): void {
    this.showFullScreenLoader = true;
    setTimeout(() => {
      this.showFullScreenLoader = false;
    }, 3000); // Auto-hide after 3 seconds
  }

  scrollTo(id: string): void {
    const element = document.getElementById(id);
    if (element) {
      const y = element.getBoundingClientRect().top + window.scrollY - 80;
      window.scrollTo({ top: y, behavior: 'smooth' });
    }
  }

  async copyCode(code: string): Promise<void> {
    try {
      await navigator.clipboard.writeText(code);
      this.toastService.success('Copied to clipboard!', { position: 'bottom-right' });
    } catch (err) {
      console.error('Failed to copy code: ', err);
      this.toastService.danger('Failed to copy code to clipboard', { position: 'bottom-right' });
    }
  }

  showSuccessToast(): void {
    this.toastService.success('Your settings have been saved successfully.');
  }

  showErrorToast(): void {
    this.toastService.danger('Connection lost. Please check your network.');
  }

  showWarningToast(): void {
    this.toastService.warning('Your session will expire in 2 minutes.');
  }

  showInfoToast(): void {
    this.toastService.info('A new software update is available.');
  }

  showActionToast(): void {
    this.toastService.info('Please review the updated privacy policy.', {
      duration: 10000,
      action: {
        label: 'Review',
        onClick: () => console.log('Action clicked!')
      }
    });
  }

  showPositionToast(): void {
    this.toastService.success('This toast appears at the top center.', {
      position: 'top-center'
    });
  }

  handleEmptyStateAction(): void {
    console.log('Empty state action clicked!');
    this.toastService.success('Action triggered from empty state.');
  }

  triggerStandardDialog() {
    this.dialogService.open({
      title: 'Action Completed',
      message: 'Your settings have been saved successfully. You can now continue using the application.',
      primaryLabel: 'Got it'
    });
  }

  triggerConfirmDialog() {
    this.dialogService.open({
      title: 'Delete Workspace?',
      message: 'Are you sure you want to delete this workspace? All data will be permanently removed. This action cannot be undone.',
      primaryLabel: 'Delete',
      secondaryLabel: 'Cancel',
      onPrimary: () => console.log('User clicked Delete!'),
      onSecondary: () => console.log('User clicked Cancel')
    });
  }

  triggerBlockingDialog() {
    this.dialogService.open({
      title: 'Session Expired',
      message: 'Your authentication session has expired due to inactivity. Please log in again to continue.',
      primaryLabel: 'Log In',
      isBlocking: true // Prevents closing via backdrop or escape key
    });
  }
}
import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Logo } from '../shared/components/logo/logo';
import { ToastService } from '../shared/components/toast/toast.service';
import { Button } from '../shared/components/button/button';
import { Badge } from '../shared/components/badge/badge';
import { Card } from '../shared/components/card/card';
import { Alert } from '../shared/components/alert/alert';
import { Tooltip } from '../shared/components/tooltip/tooltip';

interface DocSection {
  title: string;
  items: { id: string; name: string }[];
}

@Component({
  selector: 'app-testing-playground',
  imports: [CommonModule, Logo, Button, Badge, Card, Alert, Tooltip],
  templateUrl: './testing-playground.html',
  styleUrls: ['./testing-playground.scss']
})
export class TestingPlayground {
  private toastService = inject(ToastService);

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
}
import { Injectable, signal } from '@angular/core';

export type DialogVariant = 'primary' | 'secondary' | 'success' | 'danger' | 'warning';

export interface DialogOptions {
    title?: string;
    message?: string;
    messageHtml?: string;
    primaryLabel?: string;
    secondaryLabel?: string;
    primaryVariant?: DialogVariant;
    isBlocking?: boolean;
    showClose?: boolean;
    onPrimary?: () => void;
    onSecondary?: () => void;
}

@Injectable({
    providedIn: 'root'
})
export class DialogService {
    readonly currentDialog = signal<DialogOptions | null>(null);

    open(options: DialogOptions): void {
        this.currentDialog.set(options);
    }

    close(): void {
        this.currentDialog.set(null);
    }

    primaryAction(): void {
        const current = this.currentDialog();
        this.currentDialog.set(null);
        if (current?.onPrimary) {
            current.onPrimary();
        }
    }

    secondaryAction(): void {
        const current = this.currentDialog();
        this.currentDialog.set(null);
        if (current?.onSecondary) {
            current.onSecondary();
        }
    }
}
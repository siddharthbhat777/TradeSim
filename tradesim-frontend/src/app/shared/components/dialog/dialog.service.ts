import { Injectable, signal } from '@angular/core';

export interface DialogOptions {
    title?: string;
    message: string;
    confirmLabel?: string;
    cancelLabel?: string;
    isBlocking?: boolean;
    onConfirm?: () => void;
    onCancel?: () => void;
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
        const current = this.currentDialog();
        if (current?.onCancel) {
            current.onCancel();
        }
        this.currentDialog.set(null);
    }

    confirm(): void {
        const current = this.currentDialog();
        if (current?.onConfirm) {
            current.onConfirm();
        }
        this.currentDialog.set(null);
    }
}
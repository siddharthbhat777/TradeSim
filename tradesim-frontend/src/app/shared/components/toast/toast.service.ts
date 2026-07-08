import { Injectable, computed, signal } from '@angular/core';

export type ToastVariant = 'success' | 'danger' | 'warning' | 'info';

export type ToastPosition =
    | 'top-left'
    | 'top-center'
    | 'top-right'
    | 'center-left'
    | 'center-right'
    | 'bottom-left'
    | 'bottom-center'
    | 'bottom-right';

export interface ToastAction {
    label: string;
    onClick: () => void;
}

export interface ToastOptions {
    message: string;
    variant?: ToastVariant;
    duration?: number;
    position?: ToastPosition;
    action?: ToastAction;
}

export interface ToastEntry {
    id: number;
    message: string;
    variant: ToastVariant;
    duration: number;
    position: ToastPosition;
    remaining: number;
    paused: boolean;
    action?: ToastAction;
    startedAt: number;
    timeoutId?: ReturnType<typeof setTimeout>;
}

export interface ToastGroup {
    position: ToastPosition;
    entries: ToastEntry[];
}

const DEFAULT_DURATIONS: Record<ToastVariant, number> = {
    success: 4000,
    info: 4000,
    warning: 5000,
    danger: 6000,
};

const DEFAULT_POSITION: ToastPosition = 'bottom-right';

const MAX_VISIBLE = 4;

let nextToastId = 0;

@Injectable({ providedIn: 'root' })
export class ToastService {
    private readonly entries = signal<ToastEntry[]>([]);

    readonly groups = computed<ToastGroup[]>(() => {
        const byPosition = new Map<ToastPosition, ToastEntry[]>();

        for (const entry of this.entries()) {
            const list = byPosition.get(entry.position) ?? [];
            list.push(entry);
            byPosition.set(entry.position, list);
        }

        return Array.from(byPosition, ([position, list]) => ({
            position,
            entries: list.slice(0, MAX_VISIBLE),
        }));
    });

    show(options: ToastOptions): number {
        const variant = options.variant ?? 'info';
        const duration = options.duration ?? DEFAULT_DURATIONS[variant];

        const entry: ToastEntry = {
            id: nextToastId++,
            message: options.message,
            variant,
            duration,
            position: options.position ?? DEFAULT_POSITION,
            remaining: duration,
            paused: false,
            action: options.action,
            startedAt: 0,
        };

        this.entries.set([...this.entries(), entry]);
        this.syncTimers();

        return entry.id;
    }

    success(message: string, options: Omit<ToastOptions, 'message' | 'variant'> = {}): number {
        return this.show({ ...options, message, variant: 'success' });
    }

    danger(message: string, options: Omit<ToastOptions, 'message' | 'variant'> = {}): number {
        return this.show({ ...options, message, variant: 'danger' });
    }

    warning(message: string, options: Omit<ToastOptions, 'message' | 'variant'> = {}): number {
        return this.show({ ...options, message, variant: 'warning' });
    }

    info(message: string, options: Omit<ToastOptions, 'message' | 'variant'> = {}): number {
        return this.show({ ...options, message, variant: 'info' });
    }

    dismiss(id: number): void {
        const entry = this.entries().find((item) => item.id === id);

        if (entry) {
            this.clearTimer(entry);
        }

        this.entries.set(this.entries().filter((item) => item.id !== id));
        this.syncTimers();
    }

    pause(id: number): void {
        const entry = this.findVisible(id);

        if (!entry || entry.paused || !entry.timeoutId) {
            return;
        }

        entry.remaining = Math.max(entry.remaining - (Date.now() - entry.startedAt), 0);
        entry.paused = true;
        this.clearTimer(entry);
        this.entries.set([...this.entries()]);
    }

    resume(id: number): void {
        const entry = this.findVisible(id);

        if (!entry || !entry.paused) {
            return;
        }

        entry.paused = false;
        this.startTimer(entry);
        this.entries.set([...this.entries()]);
    }

    private findVisible(id: number): ToastEntry | undefined {
        for (const group of this.groups()) {
            const entry = group.entries.find((item) => item.id === id);
            if (entry) {
                return entry;
            }
        }
        return undefined;
    }

    private syncTimers(): void {
        for (const group of this.groups()) {
            for (const entry of group.entries) {
                if (!entry.timeoutId && !entry.paused) {
                    this.startTimer(entry);
                }
            }
        }
    }

    private startTimer(entry: ToastEntry): void {
        entry.startedAt = Date.now();
        entry.timeoutId = setTimeout(() => this.dismiss(entry.id), entry.remaining);
    }

    private clearTimer(entry: ToastEntry): void {
        if (entry.timeoutId) {
            clearTimeout(entry.timeoutId);
            entry.timeoutId = undefined;
        }
    }
}
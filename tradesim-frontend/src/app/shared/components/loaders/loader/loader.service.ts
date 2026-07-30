import { Injectable, signal, computed } from '@angular/core';

@Injectable({
    providedIn: 'root'
})
export class LoaderService {
    private loadingCount = signal<number>(0);

    readonly isLoading = computed(() => this.loadingCount() > 0);

    show(): void {
        this.loadingCount.update(count => count + 1);
    }

    hide(): void {
        this.loadingCount.update(count => Math.max(0, count - 1));
    }
}
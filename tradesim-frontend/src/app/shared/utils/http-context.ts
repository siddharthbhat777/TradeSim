import { HttpContext } from '@angular/common/http';
import { SKIP_ERROR_TOAST, SKIP_GLOBAL_LOADER } from '../../constants/http-context';

export interface InterceptorSkipOptions {
    loader?: boolean;
    toast?: boolean | number[];
}

export function skipInterceptors(options: InterceptorSkipOptions): HttpContext {
    const context = new HttpContext();

    if (options.loader) {
        context.set(SKIP_GLOBAL_LOADER, true);
    }

    if (options.toast !== undefined) {
        context.set(SKIP_ERROR_TOAST, options.toast);
    }

    return context;
}
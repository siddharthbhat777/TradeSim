import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
    name: 'formatCurrency',
    standalone: true
})
export class FormatCurrencyPipe implements PipeTransform {
    transform(value: number | string | null | undefined, currencyCode: string = 'INR', style: 'currency' | 'decimal' = 'currency', maxFraction: number = 2): string {
        if (value === null || value === undefined || value === '') return '0.00';

        const numValue = typeof value === 'string' ? parseFloat(value) : value;
        if (isNaN(numValue)) return '0.00';

        const locale = currencyCode === 'INR' ? 'en-IN' : 'en-US';
        const minFrac = maxFraction === 0 ? 0 : 2;
        const maxFrac = Math.max(minFrac, maxFraction);

        return new Intl.NumberFormat(locale, {
            style: style,
            currency: style === 'currency' ? currencyCode : undefined,
            minimumFractionDigits: minFrac,
            maximumFractionDigits: maxFrac
        }).format(numValue);
    }
}
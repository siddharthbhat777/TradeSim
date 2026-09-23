import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
    name: 'countryName'
})
export class CountryNamePipe implements PipeTransform {
    transform(countryCode: string | null | undefined, showFlag: boolean = false): string {
        if (!countryCode) {
            return '';
        }

        const code = countryCode.trim().toUpperCase();
        let countryName = code;

        try {
            const displayNames = new Intl.DisplayNames(['en'], { type: 'region' });
            countryName = displayNames.of(code) || code;
        } catch (e) {
            countryName = code;
        }

        if (showFlag && code.length === 2) {
            const flag = code.replace(/./g, char => String.fromCodePoint(char.charCodeAt(0) + 127397));
            return `${flag} ${countryName}`;
        }

        return countryName;
    }
}
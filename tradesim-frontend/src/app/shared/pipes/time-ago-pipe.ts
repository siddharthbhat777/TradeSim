import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'timeAgo'
})
export class TimeAgoPipe implements PipeTransform {
  transform(value: string | Date | number | null | undefined): string {
    if (!value) {
      return '';
    }

    const time = new Date(value).getTime();
    if (isNaN(time)) {
      return '';
    }

    const seconds = Math.floor((Date.now() - time) / 1000);

    if (seconds < 30) {
      return 'Just now';
    }

    const intervals: Record<string, number> = {
      year: 31536000,
      month: 2592000,
      week: 604800,
      day: 86400,
      hour: 3600,
      minute: 60,
      second: 1
    };

    for (const key in intervals) {
      const counter = Math.floor(seconds / intervals[key]);
      if (counter > 0) {
        return `${counter} ${key}${counter === 1 ? '' : 's'} ago`;
      }
    }

    return 'Just now';
  }
}
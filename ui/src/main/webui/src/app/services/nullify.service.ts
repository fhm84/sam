import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class NullifyService {
  convertEmptyStringsToNull(obj: any): any {
    if (obj === '') return null;
    if (Array.isArray(obj)) {
      return obj.map(value => this.convertEmptyStringsToNull(value));
    }
    if (obj !== null && typeof obj === 'object') {
      return Object.fromEntries(
        Object.entries(obj).map(([key, value]) => [key, this.convertEmptyStringsToNull(value)])
      );
    }
    return obj;
  }
}

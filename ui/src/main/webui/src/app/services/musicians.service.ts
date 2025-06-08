import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { map, Observable, of } from 'rxjs';
import { Musician } from '../model/datamodels';

@Injectable({
  providedIn: 'root'
})
export class MusiciansService {

  baseUrl: string = "/api";
  
  private cache = new Map<string, Musician[]>(); // Cache to store results

  constructor(private httpClient: HttpClient) { }

  getMusicians(page: number = 0, size: number = -1, nameFilter?: string): Observable<Musician[]> {
    // Create a unique cache key based on the parameters
    const cacheKey = `${page}-${size}-${nameFilter || ''}`;

    // Check if the result is already cached
    if (this.cache.has(cacheKey)) {
      return of(this.cache.get(cacheKey)!); // Return cached result as an Observable
    }

    // If not cached, make the HTTP request
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('name', nameFilter ? nameFilter : '');

    return this.httpClient.get<{ data: Musician[] }>(`${this.baseUrl}/musicians`, { params }).pipe(
      map(response => {
        const musicians = response.data || [];
        this.cache.set(cacheKey, musicians); // Cache the result
        return musicians;
      })
    );
  }
}
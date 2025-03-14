import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class SheetMusicService {

  baseUrl: string = "/api";

  constructor(private httpClient: HttpClient) { }

    addSheet(data: any): Observable<any> {
        console.log("adding new sheetMusic ("+ data.title + ")");
        var composer = data.composer;
        data.composer = {
          name: composer
          };
        return this.httpClient.post(`${this.baseUrl}/sheets`, data);
    }

    updateSheet(id: string, data: any): Observable<any> {
        console.log("updating sheetMusic ${id}");
        return this.httpClient.put(`${this.baseUrl}/sheets/${id}`, data);
    }

    getSheetList(page: number = 0, size: number = 10): Observable<any> {
      let params = new HttpParams()
            .set('page', page.toString())
            .set('size', size.toString());
        return this.httpClient.get(`${this.baseUrl}/sheets`, { params });
    }

    deleteSheet(id: string): Observable<any> {
        return this.httpClient.delete(`${this.baseUrl}/sheets/${id}`);
    }

}

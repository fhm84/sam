import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { NullifyService } from './nullify.service';

@Injectable({
  providedIn: 'root'
})
export class SheetMusicService {

  baseUrl: string = "/api";

  constructor(private nullifyService: NullifyService, private httpClient: HttpClient) { }

  addSheet(data: any): Observable<any> {
    console.log("adding new sheetMusic (" + data.title + ")");
    // FIXME: this should NOT be done here!?!
    var composer = data.composer;
    data.composer = {
      name: composer?.name || composer
    };
    // FIXME: this should NOT be done here!?!
    var arranger = data.arranger;
    data.arranger = {
      name: arranger?.name || arranger
    };

    const cleaned = this.nullifyService.convertEmptyStringsToNull(data);
    console.log("payload to add sheet: " + JSON.stringify(cleaned));
    return this.httpClient.post(`${this.baseUrl}/sheets`, cleaned);
  }

  updateSheet(id: string, data: any): Observable<any> {
    console.log('updating sheetMusic: ${id}');
    const cleaned = this.nullifyService.convertEmptyStringsToNull(data);
    console.log("payload to update sheet: " + JSON.stringify(cleaned));
    return this.httpClient.put(`${this.baseUrl}/sheets/${id}`, cleaned);
  }

  getSheetList(page: number = 0, size: number = 10): Observable<any> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.httpClient.get(`${this.baseUrl}/sheets`, { params });
  }

  loadSheet(id: string): Observable<any> {
    var sheet = this.httpClient.get(`${this.baseUrl}/sheets/${id}`);
    return sheet;
  }

  deleteSheet(id: string): Observable<any> {
    return this.httpClient.delete(`${this.baseUrl}/sheets/${id}`);
  }

  getInstrumentationsForSheet(sheetId: string): Observable<any> {
    return this.httpClient.get(`${this.baseUrl}/sheets/${sheetId}/instrumentations`);
  }

}

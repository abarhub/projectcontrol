import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {ListeVersions} from '../entity/liste-versions';
import {MajVersionApi} from '../entity/maj-version-api';

@Injectable({
  providedIn: 'root'
})
export class MajVersionService {

  private http = inject(HttpClient);
  private baseUrl = '/api/maj-version';


  getVersions(groupeId: string, nom: string): Observable<ListeVersions> {
    return this.http.get<ListeVersions>(this.baseUrl + '/' + groupeId + '/' + nom);
  }

  majVersions(groupeId: string, nom: string, majVersion: MajVersionApi): Observable<string> {
    return this.http.post<string>(this.baseUrl + '/' + groupeId + '/' + nom, majVersion);
  }
}

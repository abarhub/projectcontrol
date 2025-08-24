import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {ListeVersions} from '../entity/liste-versions';

@Injectable({
  providedIn: 'root'
})
export class MajVersionService {

  private http = inject(HttpClient);
  private baseUrl = 'http://localhost:8080/api/maj-version';


  getVersions(groupeId: string, nom: string): Observable<ListeVersions> {
    return this.http.get<ListeVersions>(this.baseUrl + '/' + groupeId + '/' + nom);
  }

}

import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class ProjetService {
  private http = inject(HttpClient);

  getProjets():Observable<Projet[]>  {
    return this.http.get<Projet[]>('http://localhost:8080/api/projet');
  }

  getProjet(nom:string):Observable<Projet>  {
    return this.http.get<Projet>('http://localhost:8080/api/projet/'+nom);
  }
}

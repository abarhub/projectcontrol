import { Routes } from '@angular/router';
import {Home} from './component/home/home';
import {Details} from './component/details/details';

export const routes: Routes = [
  { path: '', component: Home },
  { path: 'details/:groupeProjet/:nomProjet', component: Details }
];

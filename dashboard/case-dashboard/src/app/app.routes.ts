import { Routes } from '@angular/router';
import { CaseList } from './cases/case-list/case-list';
import { CaseDetail } from './cases/case-detail/case-detail';

export const routes: Routes = [
  { path: '', redirectTo: 'cases', pathMatch: 'full' },
  { path: 'cases', component: CaseList },
  { path: 'cases/:id', component: CaseDetail },
];

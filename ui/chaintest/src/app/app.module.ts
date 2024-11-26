import { NgModule } from '@angular/core';
import { BrowserModule, provideClientHydration } from '@angular/platform-browser';
import { HttpClientModule, provideHttpClient, withFetch } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { TimeagoModule } from 'ngx-timeago';
import { NgbModule, NgbOffcanvasModule, NgbTooltipModule, NgbPopoverModule, NgbDropdownModule } from '@ng-bootstrap/ng-bootstrap';
import { BaseChartDirective, provideCharts, withDefaultRegisterables } from 'ng2-charts';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { BuildListingComponent } from './pages/build/build-listing/build-listing.component';
import { FooterComponent } from './components/footer/footer.component';
import { HeaderComponent } from './components/header/header.component';
import { ProjectListingComponent } from './pages/project/project-listing/project-listing.component';
import { TestListingComponent } from './pages/test/test-listing/test-listing.component';
import { PaginateComponent } from './components/paginate/paginate.component';
import { BuildComponent } from './pages/build/build/build.component';
import { PrettyTimePipe } from './pipes/pretty-time.pipe';

@NgModule({
  declarations: [
    AppComponent,
    BuildListingComponent,
    FooterComponent,
    HeaderComponent,
    ProjectListingComponent,
    TestListingComponent,
    PaginateComponent,
    BuildComponent,
    PrettyTimePipe
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    HttpClientModule,
    FormsModule,
    NgbModule,
    TimeagoModule.forRoot(),
    BaseChartDirective
  ],
  providers: [
    provideClientHydration(),
    provideHttpClient(withFetch()),
    provideCharts(withDefaultRegisterables())
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }

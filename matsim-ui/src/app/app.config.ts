import {ApplicationConfig, importProvidersFrom, provideZoneChangeDetection} from '@angular/core';
import {provideRouter} from '@angular/router';

import {routes} from './app.routes';
import {MatButtonModule} from '@angular/material/button';
import {MatButtonToggleModule} from '@angular/material/button-toggle';
import {CommonModule} from '@angular/common';
import {provideHttpClient} from "@angular/common/http";
import {MatInputModule} from '@angular/material/input';
import {provideAnimationsAsync} from '@angular/platform-browser/animations/async';

export const appConfig: ApplicationConfig = {
    providers: [
        provideZoneChangeDetection({eventCoalescing: true}),
        provideRouter(routes),
        provideHttpClient(),
        provideAnimationsAsync(),
        importProvidersFrom(
            CommonModule,
            MatButtonModule,
            MatButtonToggleModule,
            MatInputModule,
        )
    ]
};

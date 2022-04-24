# Jeff's notes on configuring Eclipse


## Upgraded to: Version: 2022-06 M1 (4.24.0 M1), Build id: 20220414-1707

The fonts in the Package Explorer, Outline, and similar windows are *not* configurable from the standard Eclipse preferences.

I installed, from the Eclipse Marketplace, "Darkest Dark Theme with DevStyle 2021.5.26" and after several attempts to get past a null pointer exception, it seems to be working ok.  I am using the Light Gray (Fresh Light) Workbench theme, have turned on Explorer font size, and set it to 16.

The 'Problems' view has very small fonts.

Now investigating:  https://apple.stackexchange.com/questions/24621/how-to-increase-font-size-of-eclipse-globally


[This link](https://apple.stackexchange.com/questions/24621/how-to-increase-font-size-of-eclipse-globally) was very helpful:


> Inside the Eclipse.app is a setting for the font size. To make the fonts globally larger edit Eclipse.app » Contents » Eclipse » eclipse.ini and remove the line
>
> -Dorg.eclipse.swt.internal.carbon.smallFonts
>
> from the file. Save the file and restart Eclipse.

This line occurs in *two* places.  Comment out both of them by prefixing with ';' like so:
```
;-Dorg.eclipse.swt.internal.carbon.smallFonts
```

Restart eclipse and the fonts are bigger.


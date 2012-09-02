#!/usr/bin/perl
open FILE, $ARGV[0] or die $!;
%dict;
@lines = <FILE>;
$len = scalar (@lines);
for ( $i=0;$i<$len;$i++ )
{
    @words = split (/\s/,$lines[$i]); 
    $llen = scalar (@words);
    for ( $j=0;$j<$llen;$j++ )
    {
        $word =~ s/[?;:!,.'"]//g;
        if ( $word != m/^[A-Z][a-z]+/ && !$dict[$word] )
        {
            $dict[$word] =1;
        }
# $dict[$word] = 1;if ( m/^[A-Z][a-z]+(\.|:)\s/ )
    }
    foreach $key (keys %dict)
    {
        print "$key\n";
    }
}
close FILE;

$(function() { // onload...do
    $.ajax({
        method: 'GET',
        url: "repo",
        dataType: 'json',
        timeout: 4000,
        error: function (e) {
            debugger;
            console.log(e.responseText);
        },
        success: function (r) {
            var branches = r.branches;
            var commits = r.commits;
            console.log(branches);
        }
    })
});

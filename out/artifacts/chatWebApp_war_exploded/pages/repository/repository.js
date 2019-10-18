$(function() { // onload...do
    $.ajax({
        method: 'GET',
        url: "repo",
        dataType: 'json',
        timeout: 4000,
        error: function (e) {
            console.log(e.responseText);
        },
        success: function (r) {
            var branches = r.branches;
            var commits = r.commits;
            showBranchesData(branches);
        }
    })
});

function showBranchesData(branches){
    var branchesList = $('#branchesList');
    branchesList.empty();
    for (var i = 0 ; i < branches.length; i++) {
        var branch = branches[i];
        if(branch.isHead){
            branchesList.append("<li class='list-group-item list-group-item-primary'><h6>" + branch.name + "  ||  Head Branch</h6><p>" + branch.commitSha1 + "</p></li>" +
                "<small><input id=\"deleteBranch\" type=\"submit\" class=\"btn btn-primary pull-right\" value=\"Delete\" style=\"float: right;\"></small>");
        }
        else{
            branchesList.append("<li class='list-group-item list-group-item-light'><h6>" + branch.name + "</h6><p>" + branch.commitSha1 + "</p>" +
                "<input id=\"deleteBranch\" type=\"submit\" class=\"btn btn-primary pull-right\" value=\"Delete\" style=\"float: right;\"></li>");
        }
    }
}

function showCommits(commits){
    var htmlString = '<tr class="child"><td>'
        + commits["SHA1"] + '</td><td>'
        + commits["Message"] + '</td><td>'
        + commits["CreationDate"] + '</td><td>'
        + commits["CreatedBy"] + '</td><td>'
        + commits["Branches"] + '</td></tr>';
    $('#commitsTable tbody').append(htmlString);

    htmlString += '</tr>';
    var table = document.getElementById("commitsTable");
    if (table != null) {
        for (var i = 0; i < rows.length; i++) {
            htmlString += '<tr>';
            for (var j in rows[i]) {
                htmlString += '<td>' + commits[i][j] + '</td>';
            }
            htmlString += '</tr>';
        }
    }
}

